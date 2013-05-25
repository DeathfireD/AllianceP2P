package org.alliance.core.comm.filetransfers;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-26
 * Time: 21:55:56
 * To change this template use File | Settings | File Templates.
 */
public interface DataConsumer {

    void consume(ByteBuffer buf) throws IOException;
}
