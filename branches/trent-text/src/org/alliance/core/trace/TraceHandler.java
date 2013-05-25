package org.alliance.core.trace;

public abstract interface TraceHandler {

    public abstract void print(int paramInt, Object paramObject, Exception paramException);
}
