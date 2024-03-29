/*
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
package org.enodeframework.common.extensions;

import org.enodeframework.messaging.Message;

/**
 * A message monitor that returns a NoOp message callback
 */
public enum NoOpMessageMonitor implements MessageMonitor<Message> {

    /**
     * Singleton instance of a {@link NoOpMessageMonitor}.
     */
    INSTANCE;

    /**
     * Returns the instance of {@code {@link NoOpMessageMonitor}}.
     * This method is a convenience method, which can be used as a lambda expression
     *
     * @return the instance of {@code {@link NoOpMessageMonitor}}
     */
    public static NoOpMessageMonitor instance() {
        return INSTANCE;
    }

    @Override
    public MonitorCallback onMessageIngested(Message message) {
        return NoOpMessageMonitorCallback.INSTANCE;
    }
}
