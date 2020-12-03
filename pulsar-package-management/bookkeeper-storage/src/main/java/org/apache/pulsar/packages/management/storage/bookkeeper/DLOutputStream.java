/**
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
 */
package org.apache.pulsar.packages.management.storage.bookkeeper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.distributedlog.LogRecord;
import org.apache.distributedlog.api.AsyncLogWriter;
import org.apache.distributedlog.api.DistributedLogManager;

/**
 * DistributedLog Output Stream.
 */
@Slf4j
class DLOutputStream {

    private final DistributedLogManager distributedLogManager;
    private final AsyncLogWriter writer;
    private long offset = 0L;

    private DLOutputStream(DistributedLogManager distributedLogManager, AsyncLogWriter writer) {
        this.distributedLogManager = distributedLogManager;
        this.writer = writer;
    }

    static CompletableFuture<DLOutputStream> openWriterAsync(DistributedLogManager distributedLogManager) {
        log.info("Open a dlog manager");
        return distributedLogManager.openAsyncLogWriter().thenApply(w -> new DLOutputStream(distributedLogManager, w));
    }

    private CompletableFuture<List<LogRecord>> getRecords(InputStream inputStream) {
        CompletableFuture<List<LogRecord>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            byte[] readBuffer = new byte[8192];
            List<LogRecord> records = new ArrayList<>();
            try {
                int read = 0;
                while ((read = inputStream.read(readBuffer)) != -1) {
                    log.info("write something into the ledgers " + offset);
                    ByteBuf writeBuf = Unpooled.wrappedBuffer(readBuffer, 0, read);
                    offset += writeBuf.readableBytes();
                    LogRecord record = new LogRecord(offset, writeBuf);
                    records.add(record);
                }
                future.complete(records);
            } catch (IOException e) {
                log.error("Failed to get all records from the input stream", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Write all input stream data to the distribute log.
     *
     * @param inputStream the data we need to write
     * @return
     */
    CompletableFuture<DLOutputStream> writeAsync(InputStream inputStream) {
        return getRecords(inputStream)
            .thenCompose(this::writeAsync);
    }

    /**
     * Write a ByteBuf data to the distribute log.
     *
     * @param data the data we need to write
     * @return
     */
    private CompletableFuture<DLOutputStream> writeAsync(ByteBuf data) {
        synchronized (this) {
            offset += data.readableBytes();
            LogRecord record = new LogRecord(offset, data);
            log.info("execute write to the dlog " + offset);
            return writer.write(record).whenComplete((dlsn, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                } else {
                    log.info("DLSN is {} {}", dlsn.toString(), offset);
                }
            }).thenApply(ignore -> this);
        }
    }

    private CompletableFuture<DLOutputStream> writeAsync(List<LogRecord> records) {
        return writer.writeBulk(records).thenApply(ignore -> this);
    }

    /**
     * Every package will be a stream. So we need mark the stream as EndOfStream when the stream
     * write done.
     *
     * @return
     */
    CompletableFuture<Void> closeAsync() {
        return writer.markEndOfStream()
            .thenCompose(ignore -> writer.asyncClose())
            .thenCompose(ignore -> distributedLogManager.asyncClose());
    }
}

