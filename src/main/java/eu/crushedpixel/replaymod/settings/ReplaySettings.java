package eu.crushedpixel.replaymod.settings;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class ReplaySettings {

    private static final String[] CATEGORIES = new String[]{"recording", "replay", "render", "advanced"};

    public String getRecordingPath() {
        return (String) AdvancedOptions.recordingPath.getValue();
    }

    public String getRenderPath() {
        return (String) AdvancedOptions.renderPath.getValue();
    }

    public String getDownloadPath() { return (String) AdvancedOptions.downloadPath.getValue(); }

    public int getVideoFramerate() {
        return (Integer) RenderOptions.videoFramerate.getValue();
    }

    public void setVideoFramerate(int framerate) {
        RenderOptions.videoFramerate.setValue(Math.min(120, Math.max(10, framerate)));
    }

    public void toggleInterpolation() {
        ReplayOptions.linear.setValue(!((Boolean)ReplayOptions.linear.getValue()));
    }

    public boolean showRecordingIndicator() {
        return (Boolean) RecordingOptions.indicator.getValue();
    }

    public boolean isEnableRecordingServer() {
        return (Boolean) RecordingOptions.recordServer.getValue();
    }

    public boolean isEnableRecordingSingleplayer() {
        return (Boolean) RecordingOptions.recordSingleplayer.getValue();
    }

    public boolean isShowNotifications() {
        return (Boolean) RecordingOptions.notifications.getValue();
    }

    public boolean isLinearMovement() {
        return (Boolean) ReplayOptions.linear.getValue();
    }

    public boolean showPathPreview() { return (Boolean) ReplayOptions.previewPath.getValue(); }

    public void setShowPathPreview(boolean show) {
        ReplayOptions.previewPath.setValue(show);
    }

    public boolean showClearKeyframesCallback() {
        return (Boolean) ReplayOptions.keyframeCleanCallback.getValue();
    }

    private Property getConfigSetting(Configuration config, String name, Object value, String category, boolean warning) {
        if(warning) {
            String warningMsg = "Please be careful when modifying this setting, as setting it to an invalid value might harm your computer.";
            if(value instanceof Integer) {
                return config.get(category, name, (Integer) value, warningMsg);
            } else if(value instanceof Boolean) {
                return config.get(category, name, (Boolean) value, warningMsg);
            } else if(value instanceof Double) {
                return config.get(category, name, (Double) value, warningMsg);
            } else if(value instanceof Float) {
                return config.get(category, name, (double) (Float) value, warningMsg);
            } else if(value instanceof String) {
                return config.get(category, name, (String) value, warningMsg);
            }
        } else {
            if(value instanceof Integer) {
                return config.get(category, name, (Integer) value);
            } else if(value instanceof Boolean) {
                return config.get(category, name, (Boolean) value);
            } else if(value instanceof Double) {
                return config.get(category, name, (Double) value);
            } else if(value instanceof Float) {
                return config.get(category, name, (double) (Float) value);
            } else if(value instanceof String) {
                return config.get(category, name, (String) value);
            }
        }
        return null;
    }

    private Object getValueObject(Property p) {
        if(p.isIntValue()) {
            return p.getInt();
        } else if(p.isDoubleValue()) {
            return p.getDouble();
        } else if(p.isBooleanValue()) {
            return p.getBoolean();
        } else {
            return p.getString();
        }
    }

    public enum RecordingOptions implements ValueEnum {
        recordServer(true, "replaymod.gui.settings.recordserver"),
        recordSingleplayer(true, "replaymod.gui.settings.recordsingleplayer"),
        notifications(true, "replaymod.gui.settings.notifications"), // TODO
        indicator(true, "replaymod.gui.settings.indicator");

        private Object value;
        private String name;

        RecordingOptions(Object value, String name) {
            this.value = value;
            this.name = name;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public String getName() { return I18n.format(name); }
    }

    public enum ReplayOptions implements ValueEnum {
        linear(false, "replaymod.gui.settings.interpolation"),
        previewPath(false, "replaymod.gui.settings.pathpreview"),
        keyframeCleanCallback(true, "replaymod.gui.settings.keyframecleancallback"),
        showChat(false, "options.chat.visibility"),
        renderInvisible(true, "replaymod.gui.settings.renderinvisible");

        private Object value;
        private String name;

        ReplayOptions(Object value, String name) {
            this.value = value;
            this.name = name;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public String getName() { return I18n.format(name); }
    }

    public enum RenderOptions implements ValueEnum {
        videoFramerate(30, "replaymod.gui.settings.framerate"),
        waitForChunks(true, "replaymod.gui.settings.forcechunks");

        private Object value;
        private String name;

        RenderOptions(Object value, String name) {
            this.value = value;
            this.name = name;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public String getName() { return I18n.format(name); }
    }

    public enum AdvancedOptions implements ValueEnum {
        recordingPath("./replay_recordings/", ""),
        renderPath("./replay_videos/", ""),
        downloadPath("./replay_downloads", ""),
        disableLoginPrompt(false, "");

        private Object value;
        private String name;

        AdvancedOptions(Object value, String name) {
            this.value = value;
            this.name = name;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public String getName() { return I18n.format(name); }
    }

    public interface ValueEnum {
        Object getValue();

        void setValue(Object value);

        String getName();
    }
}
