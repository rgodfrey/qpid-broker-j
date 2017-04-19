/*
*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/


package org.apache.qpid.server.protocol.v1_0.type.transport;


import java.nio.ByteBuffer;

import org.apache.qpid.server.protocol.v1_0.ConnectionHandler;
import org.apache.qpid.server.protocol.v1_0.type.FrameBody;
import org.apache.qpid.server.protocol.v1_0.type.CompositeTypeField;
import org.apache.qpid.server.protocol.v1_0.type.UnsignedInteger;

public class Detach implements FrameBody
{
    private ByteBuffer _payload;

    @CompositeTypeField(mandatory = true)
    private UnsignedInteger _handle;

    @CompositeTypeField
    private Boolean _closed;

    @CompositeTypeField
    private Error _error;

    public UnsignedInteger getHandle()
    {
        return _handle;
    }

    public void setHandle(UnsignedInteger handle)
    {
        _handle = handle;
    }

    public Boolean getClosed()
    {
        return _closed;
    }

    public void setClosed(Boolean closed)
    {
        _closed = closed;
    }

    public Error getError()
    {
        return _error;
    }

    public void setError(Error error)
    {
        _error = error;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("Detach{");
        final int origLength = builder.length();

        if (_handle != null)
        {
            if (builder.length() != origLength)
            {
                builder.append(',');
            }
            builder.append("handle=").append(_handle);
        }

        if (_closed != null)
        {
            if (builder.length() != origLength)
            {
                builder.append(',');
            }
            builder.append("closed=").append(_closed);
        }

        if (_error != null)
        {
            if (builder.length() != origLength)
            {
                builder.append(',');
            }
            builder.append("error=").append(_error);
        }

        builder.append('}');
        return builder.toString();
    }

    public void invoke(short channel, ConnectionHandler conn)
    {
        conn.receiveDetach(channel, this);
    }

    public ByteBuffer getPayload()
    {
        return _payload;
    }

    public void setPayload(ByteBuffer payload)
    {
        _payload = payload;
    }
}