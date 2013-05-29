package org.alliance.core.file.filedatabase;

import org.alliance.core.Language;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-02
 * Time: 19:51:22
 * To change this template use File | Settings | File Templates.
 */
public enum FileType {

    EVERYTHING("everything", 0, new FileTypeIdentifier() {

@Override
public boolean matches(String s) {
    return true;
}
}),
    AUDIO("audio", 1, new ExtensionFileType(new String[]{"mp3", "wav", "flac", "acc", "ogg", "asf", "wma", "aif", "aiff", "mid", "midi", "ra"})),
    ARCHIVE("archive", 4, new ExtensionFileType(new String[]{"rar", "zip", "zipx", "tar", "gz", "7z", "tar", "tgz", "sit", "sitx"}) {

@Override
public boolean matches(String s) {
    if (!super.matches(s)) {
        if (s.length() > 3 && s.charAt(s.length() - 3) == 'r' && Character.isDigit(s.charAt(s.length() - 2)) && Character.isDigit(s.charAt(s.length() - 1))) {
            return true;
        }
        return false;
    }
    return true;
}
}),
    CDDVD("cddvd", 3, new ExtensionFileType(new String[]{"iso", "img", "bin", "cue", "mdf", "mds", "dmg", "vcd"})),
    DOCUMENT("document", 6, new ExtensionFileType(new String[]{"txt", "nfo", "doc", "docx", "rtf", "wpd", "wps", "pdf", "xlr", "xls", "xlsx", "mobi", "lit"})),
    IMAGE("image", 5, new ExtensionFileType(new String[]{"jpg", "jpeg", "gif", "png", "bmp", "tif", "tiff", "psd", "svg", "ai", "eps", "ps"})),
    PRESENTATION("presentation", 7, new ExtensionFileType(new String[]{"ppt", "pptx", "pps", "key"})),
    VIDEO("video", 2, new ExtensionFileType(new String[]{"avi", "mkv", "mpg", "mpeg", "mov", "mp4", "asf", "wmv", "divx", "xvid", "rmvb", "rm", "ogm", "flv", "vob"}));
    private final String description;
    private final byte id;
    private final FileTypeIdentifier fileTypeIdentifier;

    FileType(String description, int id, FileTypeIdentifier fileTypeIdentifier) {
        this.description = Language.getLocalizedString(getClass(), description);
        this.id = (byte) id;
        this.fileTypeIdentifier = fileTypeIdentifier;
    }

    public byte id() {
        return id;
    }

    public FileTypeIdentifier fileTypeIdentifier() {
        return fileTypeIdentifier;
    }

    public String description() {
        return description;
    }

    public static int indexOf(FileType ft) {
        for (int i = 0; i < values().length; i++) {
            if (ft == values()[i]) {
                return i;
            }
        }
        return -1;
    }

    public static FileType getFileTypeById(int id) {
        for (int i = 0; i < values().length; i++) {
            if (id == values()[i].id) {
                return values()[i];
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "FileType " + description + " (" + id + ")";
    }

    public static FileType getByFileName(String filename) {
        for (FileType t : values()) {
            if (t == EVERYTHING) {
                continue;
            }
            if (t.fileTypeIdentifier().matches(filename)) {
                return t;
            }
        }
        return EVERYTHING;
    }
}
