package io.gdx.cdda.bn.nextgen.tileset.load;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.tileset.TilesetConfigLoader;
import io.gdx.cdda.bn.nextgen.tileset.TilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.atlas.DynamicSheetUploader;
import io.gdx.cdda.bn.nextgen.tileset.atlas.PixmapSheetLoader;
import io.gdx.cdda.bn.nextgen.tileset.atlas.SheetTextureUploader;
import io.gdx.cdda.bn.nextgen.tileset.atlas.TransparencyKey;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetEntry;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetMerger;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileInfo;
import io.gdx.cdda.bn.nextgen.tileset.validate.PostLoadValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Incremental tileset load for the render thread (one GPU chunk per {@link #step()}). */
public final class TilesetLoadSession {

    private enum Phase {
        CONFIG,
        SHEET_DECODE,
        SHEET_UPLOAD,
        SHEET_REGISTER,
        MOD_NEXT,
        FINALIZE,
        DONE,
        FAILED,
        CANCELLED
    }

    private static final class SheetWork {
        private final Path imagePath;
        private final JsonValue sheetConfig;
        private final int spriteWidth;
        private final int spriteHeight;
        private final TransparencyKey transparencyKey;
        private final int spriteIdOffset;
        private final String progressLabel;
        private final boolean legacySheet;

        private SheetWork(
            final Path imagePath,
            final JsonValue sheetConfig,
            final int spriteWidth,
            final int spriteHeight,
            final TransparencyKey transparencyKey,
            final int spriteIdOffset,
            final String progressLabel,
            final boolean legacySheet
        ) {
            this.imagePath = imagePath;
            this.sheetConfig = sheetConfig;
            this.spriteWidth = spriteWidth;
            this.spriteHeight = spriteHeight;
            this.transparencyKey = transparencyKey;
            this.spriteIdOffset = spriteIdOffset;
            this.progressLabel = progressLabel;
            this.legacySheet = legacySheet;
        }
    }

    private final TilesetRegistry registry;
    private final String tilesetId;
    private final TilesetLoadOptions options;
    private final List<ModTilesetEntry> modEntries;

    private Phase phase = Phase.CONFIG;
    private TilesetLoadContext context;
    private TilesetConfigLoader.LoadedTilesetConfig loadedConfig;
    private TileInfo tileInfo;
    private LoadedTileset result;
    private String errorMessage = "";
    private String progressLabel = "";
    private String progressDetail = "";

    private List<SheetWork> sheetQueue = new ArrayList<>();
    private int sheetIndex;
    private SheetWork activeSheet;
    private Pixmap activePixmap;
    private SheetTextureUploader.IncrementalUpload activeUpload;
    private int activeSheetTileCount;
    private int modIndex;
    private int totalSheetCount;
    private int completedSheetCount;

    private TilesetLoadSession(
        final TilesetRegistry registry,
        final String tilesetId,
        final TilesetLoadOptions options,
        final ModTilesetRegistry modTilesets
    ) {
        this.registry = registry;
        this.tilesetId = tilesetId;
        this.options = options;
        this.modEntries = new ArrayList<>();
        for (final ModTilesetEntry entry : modTilesets.getEntries()) {
            if (entry.isCompatible(tilesetId)) {
                modEntries.add(entry);
            }
        }
    }

    public static TilesetLoadSession start(
        final TilesetRegistry registry,
        final String tilesetId,
        final TilesetLoadOptions options,
        final ModTilesetRegistry modTilesets
    ) {
        return new TilesetLoadSession(registry, tilesetId, options, modTilesets);
    }

    public String getProgressTitle() {
        return progressLabel;
    }

    public String getProgressDetail() {
        return progressDetail;
    }

    public String getProgressLabel() {
        final String summary = getProgressSummary();
        if (!summary.isEmpty()) {
            return summary;
        }
        if (progressDetail.isEmpty()) {
            return progressLabel;
        }
        return progressLabel + "  " + progressDetail;
    }

    /** User-facing load status: overall sheet progress, not per-GPU-chunk detail. */
    public String getProgressSummary() {
        switch (phase) {
            case CONFIG:
                return "Reading tileset config";
            case FINALIZE:
                return "Finalizing";
            case MOD_NEXT:
                return formatSheetProgress("Loading mod tilesets");
            case SHEET_DECODE:
            case SHEET_UPLOAD:
            case SHEET_REGISTER:
                return formatActiveSheetProgress();
            default:
                return "";
        }
    }

    private String formatActiveSheetProgress() {
        final StringBuilder summary = new StringBuilder(formatSheetProgress(null));
        if (activeSheet != null && activeSheet.progressLabel != null && !activeSheet.progressLabel.isEmpty()) {
            if (summary.length() > 0) {
                summary.append(" — ");
            }
            summary.append(activeSheet.progressLabel);
        }
        if (phase == Phase.SHEET_UPLOAD
            && activeUpload != null
            && activeUpload.getTotalChunkCount() > 1) {
            summary.append(" (GPU ")
                .append(activeUpload.getUploadedChunkCount())
                .append('/')
                .append(activeUpload.getTotalChunkCount())
                .append(')');
        }
        return summary.toString();
    }

    private String formatSheetProgress(final String fallback) {
        if (totalSheetCount <= 0) {
            return fallback != null ? fallback : "";
        }
        final int current = Math.min(completedSheetCount + 1, totalSheetCount);
        return "Sheet " + current + "/" + totalSheetCount;
    }

    public boolean isComplete() {
        return phase == Phase.DONE;
    }

    public boolean isFailed() {
        return phase == Phase.FAILED;
    }

    public boolean isCancelled() {
        return phase == Phase.CANCELLED;
    }

    public boolean isActive() {
        return phase != Phase.DONE && phase != Phase.FAILED && phase != Phase.CANCELLED;
    }

    public LoadedTileset getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** @return {@code true} while more steps remain */
    public boolean step() {
        if (!isActive()) {
            return false;
        }
        try {
            return advance();
        } catch (final IOException e) {
            markFailed(e.getMessage());
            return false;
        } catch (final RuntimeException e) {
            markFailed(e.getMessage());
            return false;
        }
    }

    public void cancel() {
        if (!isActive()) {
            return;
        }
        phase = Phase.CANCELLED;
        disposeActivePixmap();
        if (context != null) {
            context.getTextures().dispose();
            context = null;
        }
    }

    private boolean advance() throws IOException {
        switch (phase) {
            case CONFIG:
                beginConfig();
                return true;
            case SHEET_DECODE:
                beginSheetDecode();
                return true;
            case SHEET_UPLOAD:
                return advanceSheetUpload();
            case SHEET_REGISTER:
                finishSheetRegister();
                return true;
            case MOD_NEXT:
                return beginNextMod();
            case FINALIZE:
                finishLoad();
                return false;
            default:
                return false;
        }
    }

    private void beginConfig() throws IOException {
        loadedConfig = TilesetConfigLoader.loadConfig(registry, tilesetId);
        tileInfo = TilesetLoader.parseTileInfo(loadedConfig.getConfigRoot());
        context = TilesetLoadContext.create(tileInfo, options);
        final TilesetConfigLoader.ResolvedTilesetPaths paths = loadedConfig.getPaths();
        progressLabel = "Loading " + tilesetId;
        totalSheetCount = countSheets(loadedConfig.getConfigRoot());
        for (final ModTilesetEntry entry : modEntries) {
            final JsonValue modConfig = openModConfig(entry);
            if (modConfig != null) {
                totalSheetCount += countSheets(modConfig);
            }
        }
        completedSheetCount = 0;
        sheetQueue = buildSheetWorks(
            loadedConfig.getConfigRoot(),
            paths.getTilesetRoot(),
            paths.getImagePath(),
            0
        );
        sheetIndex = 0;
        modIndex = 0;
        phase = sheetQueue.isEmpty() ? Phase.MOD_NEXT : Phase.SHEET_DECODE;
    }

    private void beginSheetDecode() throws IOException {
        activeSheet = sheetQueue.get(sheetIndex);
        progressLabel = "Loading " + tilesetId + ": " + activeSheet.progressLabel
            + " (" + (sheetIndex + 1) + "/" + sheetQueue.size() + ")";
        progressDetail = "decoding";
        activePixmap = PixmapSheetLoader.load(activeSheet.imagePath, activeSheet.transparencyKey);
        if (context.getTextures().isDynamicAtlas()) {
            activeSheetTileCount = DynamicSheetUploader.uploadSheet(
                activePixmap,
                activeSheet.spriteWidth,
                activeSheet.spriteHeight,
                context.getOffset(),
                context.getTextures().getDynamicAtlas(),
                context.getTextures().getTileLookup()
            );
            activePixmap.dispose();
            activePixmap = null;
            phase = Phase.SHEET_REGISTER;
            return;
        }
        activeUpload = SheetTextureUploader.IncrementalUpload.begin(
            activePixmap,
            activeSheet.spriteWidth,
            activeSheet.spriteHeight,
            context.getOffset(),
            options.getMaxTextureWidth(),
            options.getMaxTextureHeight(),
            context.getTextures().getBakedTables(),
            options
        );
        progressDetail = "upload 0/" + activeUpload.getTotalChunkCount();
        phase = Phase.SHEET_UPLOAD;
    }

    private boolean advanceSheetUpload() {
        if (activeUpload.step()) {
            progressDetail = "upload " + activeUpload.getUploadedChunkCount()
                + "/" + activeUpload.getTotalChunkCount();
            return true;
        }
        activeSheetTileCount = activeUpload.getExpectedTileCount();
        activeUpload.disposePixmap();
        activeUpload = null;
        activePixmap = null;
        phase = Phase.SHEET_REGISTER;
        return true;
    }

    private void finishSheetRegister() throws IOException {
        context.addOffset(activeSheetTileCount);
        context.registerSheetAfterUpload(
            activeSheet.sheetConfig,
            activeSheet.spriteIdOffset,
            activeSheet.legacySheet
        );
        sheetIndex++;
        completedSheetCount++;
        progressDetail = "";
        if (sheetIndex < sheetQueue.size()) {
            phase = Phase.SHEET_DECODE;
            return;
        }
        phase = Phase.MOD_NEXT;
    }

    private boolean beginNextMod() throws IOException {
        while (modIndex < modEntries.size()) {
            final ModTilesetEntry entry = modEntries.get(modIndex++);
            final JsonValue modConfig = openModConfig(entry);
            if (modConfig == null) {
                continue;
            }
            final int spriteIdOffset = context.getOffset();
            sheetQueue = buildSheetWorks(
                modConfig,
                entry.getBasePath(),
                loadedConfig.getPaths().getImagePath(),
                spriteIdOffset
            );
            sheetIndex = 0;
            if (!sheetQueue.isEmpty()) {
                phase = Phase.SHEET_DECODE;
                return true;
            }
        }
        phase = Phase.FINALIZE;
        return true;
    }

    private void finishLoad() {
        context.finishAtlasLoad();
        final PostLoadValidator.Result validated = PostLoadValidator.validate(
            context.getTiles(),
            context.getTextures(),
            context.getOffset(),
            tileInfo,
            options
        );
        result = new LoadedTileset(
            tilesetId,
            tileInfo,
            loadedConfig.getPaths(),
            context.getTextures(),
            context.getTiles(),
            context.getStateModifiers().getGroups(),
            validated.getSpriteCount(),
            validated.getWarnings()
        );
        context = null;
        progressLabel = "";
        progressDetail = "";
        phase = Phase.DONE;
    }

    private void markFailed(final String message) {
        errorMessage = message;
        phase = Phase.FAILED;
        disposeActivePixmap();
        if (context != null) {
            context.getTextures().dispose();
            context = null;
        }
    }

    private void disposeActivePixmap() {
        if (activeUpload != null) {
            activeUpload.disposePixmap();
            activeUpload = null;
        }
        if (activePixmap != null) {
            activePixmap.dispose();
            activePixmap = null;
        }
    }

    private List<SheetWork> buildSheetWorks(
        final JsonValue config,
        final Path tilesetRoot,
        final Path legacyImagePath,
        final int spriteIdOffset
    ) throws IOException {
        final List<SheetWork> works = new ArrayList<>();
        if (config.has("tiles-new")) {
            for (JsonValue sheet = config.get("tiles-new").child; sheet != null; sheet = sheet.next) {
                final String fileName = sheet.getString("file", "");
                if (fileName.isEmpty()) {
                    throw new IOException("tiles-new sheet missing required \"file\" under " + tilesetRoot);
                }
                final Path imagePath = tilesetRoot.resolve(fileName).normalize();
                works.add(new SheetWork(
                    imagePath,
                    sheet,
                    sheet.getInt("sprite_width", tileInfo.getWidth()),
                    sheet.getInt("sprite_height", tileInfo.getHeight()),
                    TilesetLoadContext.transparencyForSheet(sheet),
                    spriteIdOffset,
                    fileName,
                    false
                ));
            }
        } else if (config.has("tiles")) {
            final Path imagePath = legacyImagePath != null ? legacyImagePath : tilesetRoot;
            works.add(new SheetWork(
                imagePath,
                config,
                tileInfo.getWidth(),
                tileInfo.getHeight(),
                TransparencyKey.disabled(),
                spriteIdOffset,
                imagePath.getFileName().toString(),
                true
            ));
        }
        return works;
    }

    private static int countSheets(final JsonValue config) {
        if (config.has("tiles-new")) {
            int count = 0;
            for (JsonValue sheet = config.get("tiles-new").child; sheet != null; sheet = sheet.next) {
                count++;
            }
            return count;
        }
        if (config.has("tiles")) {
            return 1;
        }
        return 0;
    }

    private static JsonValue openModConfig(final ModTilesetEntry entry) throws IOException {
        final Path jsonPath = entry.getFullPath();
        if (!Files.isRegularFile(jsonPath)) {
            throw new IOException("Failed to open mod tileset json: " + jsonPath);
        }
        final byte[] bytes = Files.readAllBytes(jsonPath);
        final String text = new String(bytes, StandardCharsets.UTF_8);
        final JsonValue root = new JsonReader().parse(text);
        return ModTilesetMerger.resolveConfigObject(root, entry.getNumInFile());
    }
}
