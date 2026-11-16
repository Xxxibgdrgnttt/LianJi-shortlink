/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.shortlink.project.toolkit;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务生命周期辅助工具
 */
@Slf4j
public final class TransactionUtil {

    private TransactionUtil() {
    }

    /**
     * 在事务提交后执行，没有事务时立即执行
     */
    public static void executeAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /**
     * 在事务完成（提交或回滚）后释放锁，确保锁覆盖数据库事务的提交窗口
     */
    public static void unlockAfterCompletion(RLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            unlockSafely(lock);
            return;
        }
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    unlockSafely(lock);
                }
            });
        } catch (RuntimeException ex) {
            unlockSafely(lock);
            throw ex;
        }
    }

    private static void unlockSafely(RLock lock) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception ex) {
            log.error("事务结束后释放 Redisson 锁失败", ex);
        }
    }
}
