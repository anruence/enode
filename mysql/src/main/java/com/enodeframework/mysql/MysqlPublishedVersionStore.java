package com.enodeframework.mysql;

import com.enodeframework.ObjectContainer;
import com.enodeframework.common.io.AsyncTaskResult;
import com.enodeframework.common.io.AsyncTaskStatus;
import com.enodeframework.common.utilities.Ensure;
import com.enodeframework.configurations.DataSourceKey;
import com.enodeframework.configurations.DefaultDBConfigurationSetting;
import com.enodeframework.configurations.OptionSetting;
import com.enodeframework.eventing.IPublishedVersionStore;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * @author anruence@gmail.com
 */
public class MysqlPublishedVersionStore implements IPublishedVersionStore {
    private static final Logger logger = LoggerFactory.getLogger(MysqlPublishedVersionStore.class);
    private final SQLClient sqlClient;
    private final String tableName;
    private final String uniqueIndexName;

    public MysqlPublishedVersionStore(DataSource ds, OptionSetting optionSetting) {
        Ensure.notNull(ds, "ds");
        if (optionSetting != null) {
            tableName = optionSetting.getOptionValue(DataSourceKey.PUBLISHED_VERSION_TABLENAME);
            uniqueIndexName = optionSetting.getOptionValue(DataSourceKey.PUBLISHED_VERSION_UNIQUE_INDEX_NAME);
        } else {
            DefaultDBConfigurationSetting setting = new DefaultDBConfigurationSetting();
            tableName = setting.getPublishedVersionTableName();
            uniqueIndexName = setting.getPublishedVersionUniqueIndexName();
        }
        Ensure.notNull(tableName, "tableName");
        Ensure.notNull(uniqueIndexName, "uniqueIndexName");
        sqlClient = JDBCClient.create(ObjectContainer.vertx, ds);
    }

    @Override
    public CompletableFuture<AsyncTaskResult> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId, int publishedVersion) {
        CompletableFuture<AsyncTaskResult> future = new CompletableFuture<>();
        String sql = "";
        JsonArray array = new JsonArray();
        if (publishedVersion == 1) {
            sql = String.format("INSERT INTO %s(ProcessorName,AggregateRootTypeName,AggregateRootId,Version,CreatedOn) VALUES(?,?,?,?,?)", tableName);
            array.add(processorName);
            array.add(aggregateRootTypeName);
            array.add(aggregateRootId);
            array.add(1);
            array.add(new Date().toInstant());
        } else {
            sql = String.format("UPDATE %s set Version=?,CreatedOn=? WHERE ProcessorName=? and AggregateRootId=? and Version=?", tableName);
            array.add(publishedVersion);
            array.add(new Date().toInstant());
            array.add(processorName);
            array.add(aggregateRootId);
            array.add(publishedVersion - 1);
        }
        sqlClient.updateWithParams(sql, array, x -> {
            if (x.succeeded()) {
                future.complete(AsyncTaskResult.Success);
                return;
            }
            future.completeExceptionally(x.cause());
        });
        return future.exceptionally(throwable -> {
            if (throwable instanceof SQLException) {
                SQLException ex = (SQLException) throwable;
                if (ex.getErrorCode() == 1062 && ex.getMessage().contains(uniqueIndexName)) {
                    future.complete(AsyncTaskResult.Success);
                }
                logger.error("Insert or update aggregate published version has sql exception.", ex);
                return new AsyncTaskResult(AsyncTaskStatus.IOException, ex.getMessage());
            }
            logger.error("Insert or update aggregate published version has unknown exception.", throwable);
            return new AsyncTaskResult(AsyncTaskStatus.Failed, throwable.getMessage());
        });
    }

    @Override
    public CompletableFuture<AsyncTaskResult<Integer>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId) {
        CompletableFuture<AsyncTaskResult<Integer>> future = new CompletableFuture<>();
        String sql = String.format("SELECT Version FROM %s WHERE ProcessorName=? AND AggregateRootId=?", tableName);
        JsonArray array = new JsonArray();
        array.add(processorName);
        array.add(aggregateRootId);
        sqlClient.queryWithParams(sql, array, x -> {
            if (x.succeeded()) {
                int result = 0;
                if (x.result().getNumRows() >= 1) {
                    result = x.result().getRows().get(0).getInteger("Version");
                }
                future.complete(new AsyncTaskResult<>(AsyncTaskStatus.Success, result));
                return;
            }
            future.completeExceptionally(x.cause());
        });
        return future.exceptionally(throwable -> {
            if (throwable instanceof SQLException) {
                SQLException ex = (SQLException) throwable;
                logger.error("Get aggregate published version has sql exception.", ex);
                return new AsyncTaskResult<>(AsyncTaskStatus.IOException, ex.getMessage());
            }
            logger.error("Get aggregate published version has unknown exception.", throwable);
            return new AsyncTaskResult<>(AsyncTaskStatus.Failed, throwable.getMessage());
        });
    }
}
