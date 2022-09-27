/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package source.debezium;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.engine.spi.OffsetCommitPolicy;
import io.hstream.HRecord;
import io.hstream.io.SourceTaskContext;
import java.io.IOException;
import java.util.Properties;
import io.hstream.io.SourceTask;
import java.util.UUID;

abstract class DebeziumSourceTask implements SourceTask {
    DebeziumEngine<ChangeEvent<String, String>> engine;
    SourceTaskContext ctx;
    Properties props = new Properties();

    @Override
    public void run(HRecord cfg, SourceTaskContext ctx) {
        try {
            System.out.println("cfg:" + cfg.toJsonString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.ctx = ctx;
        props.setProperty("name", "engine");
        OffsetBackingStore.setKvStore(ctx.getKvStore());
        props.setProperty("offset.storage", "source.debezium.OffsetBackingStore");
        props.setProperty("offset.flush.interval.ms", "10000");

        // schema
        props.setProperty("key.converter", "org.apache.kafka.connect.json.JsonConverter");
        props.setProperty("key.converter.schemas.enable", "false");
        props.setProperty("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        props.setProperty("value.converter.schemas.enable", "false");

        props.setProperty("database.hostname", cfg.getString("host"));
        props.setProperty("database.port", String.valueOf(cfg.getInt("port")));
        props.setProperty("database.user", cfg.getString("user"));
        props.setProperty("database.password", cfg.getString("password"));

        var namespace = UUID.randomUUID().toString().replace("-", "");
//        if (cfg.contains("namespace")) {
//            namespace = cfg.getString("namespace");
//        }
        props.setProperty("database.server.name", namespace);
        DatabaseHistory.setKv(ctx.getKvStore());
        props.setProperty("database.history", "source.debezium.DatabaseHistory");

        // transforms
        props.setProperty("transforms", "unwrap");
        props.setProperty("transforms.unwrap.type", "io.debezium.transforms.ExtractNewRecordState");
        props.setProperty("transforms.unwrap.drop.tombstones", "false");

        // Create the engine with this configuration ...
        engine = DebeziumEngine.create(Json.class)
                .using(props)
                .using(OffsetCommitPolicy.always())
                .notifying(new RecordConsumer(ctx, namespace, cfg.getString("stream")))
                .build();

        engine.run();
    }

    @Override
    public void stop() {
        try {
            engine.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}