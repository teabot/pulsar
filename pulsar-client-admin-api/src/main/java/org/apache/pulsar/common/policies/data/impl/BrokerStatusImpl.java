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
package org.apache.pulsar.common.policies.data.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.pulsar.common.policies.data.BrokerStatus;

/**
 * Information about the broker status.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public final class BrokerStatusImpl implements BrokerStatus {
    private String brokerAddress;
    private boolean active;
    private int loadFactor;

    public static BrokerStatusImplBuilder builder() {
        return new BrokerStatusImplBuilder();
    }

    @Override
    public int compareTo(BrokerStatus other) {
        int result = Integer.compare(this.loadFactor, other.getLoadFactor());
        if (result == 0) {
            result = this.brokerAddress.compareTo(other.getBrokerAddress());
        }
        return result;
    }

    public static class BrokerStatusImplBuilder implements BrokerStatus.Builder {
        private String brokerAddress;
        private boolean active;
        private int loadFactor;

        public BrokerStatusImplBuilder brokerAddress(String brokerAddress) {
            this.brokerAddress = brokerAddress;
            return this;
        }

        public BrokerStatusImplBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public BrokerStatusImplBuilder loadFactor(int loadFactor) {
            this.loadFactor = loadFactor;
            return this;
        }

        public BrokerStatusImpl build() {
            return new BrokerStatusImpl(brokerAddress, active, loadFactor);
        }
    }
}
