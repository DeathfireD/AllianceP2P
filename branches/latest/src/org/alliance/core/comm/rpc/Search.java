package org.alliance.core.comm.rpc;

import org.alliance.core.T;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.file.filedatabase.FileType;
import static org.alliance.core.comm.rpc.SearchHitsV2.MAX_SEARCH_HITS;

import java.io.IOException;
import java.util.ArrayList;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.file.filedatabase.ExtensionFileType;

/**
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class Search extends RPC {

    private String query;
    private byte fileTypeId;

    public Search() {
    }

    public Search(String query, byte fileTypeId) {
        this();
        this.query = query;
        this.fileTypeId = fileTypeId;
    }

    @Override
    public void execute(Packet in) throws IOException {
        query = in.readUTF();

        core.logNetworkEvent("Search query " + query + " from " + con.getRemoteFriend());

        // FileType ft = FileType.EVERYTHING;
        byte type = in.readByte();
        /* if (FileType.getFileTypeById(type) != null) {
        ft = FileType.getFileTypeById(type);
        if (ft.fileTypeIdentifier() instanceof ExtensionFileType) {
        ExtensionFileType extft = (ExtensionFileType) ft.fileTypeIdentifier();
        String[] extensions = extft.getExtensions();
        for(String ex : extensions){
        System.out.println(ex);
        }
        }
        }*/

        //TODO FileTYPE searching
        SearchHitsV2 sh = new SearchHitsV2();
        ArrayList<SearchHit> hitList = core.getShareManager().getFileDatabase().getSearchHits(query, type, MAX_SEARCH_HITS);
        for (SearchHit hit : hitList) {
            sh.addHit(hit);
        }

        if (T.t) {
            T.info("Found " + sh.getNHits() + " hits.");
        }
        if (sh.getNHits() > 0) {
            //reply with Search hits
            send(fromGuid, sh);
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeUTF(query);
        p.writeByte(fileTypeId);
        return p;
    }
}
