package org.alliance.core.file.filedatabase;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-02
 * Time: 19:51:22
 * To change this template use File | Settings | File Templates.
 */
public enum FileType {

    EVERYTHING("All Files", 0, new FileTypeIdentifier() {

@Override
public boolean matches(String s) {
    return true;
}
}),
    AUDIO("Audio", 1, new ExtensionFileType(new String[]{"mp3", "mp4", "wav", "acc", "ogg", "asf", "wma", "aiff"})),
    ARCHIVE("Archives", 4, new ExtensionFileType(new String[]{"rar", "zip", "tar", "gz", "7z"}) {

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
    CDDVD("CD/DVD Images", 3, new ExtensionFileType(new String[]{"iso", "img", "bin", "cue", "mdf", "mds"})),
    DOCUMENT("Documents", 6, new ExtensionFileType(new String[]{"doc", "txt", "nfo"})),
    IMAGE("Pictures", 5, new ExtensionFileType(new String[]{"jpg", "jpeg", "gif", "png", "bmp", "tiff"})),
    PRESENTATION("Presentations", 7, new ExtensionFileType(new String[]{"key", "pps", "ppt"})),
    VIDEO("Video", 2, new ExtensionFileType(new String[]{"avi", "mkv", "mpg", "mpeg", "mov", "asf", "wmv", "divx", "xvid", "rmvb", "rm", "ogm"}));
    private final String description;
    private final byte id;
    private final FileTypeIdentifier fileTypeIdentifier;

    FileType(String description, int id, FileTypeIdentifier fileTypeIdentifier) {
        this.description = description;
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
