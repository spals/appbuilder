package net.spals.appbuilder.app.examples.grpc.rest.web;

import com.google.inject.Inject;
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.grpc.stub.StreamObserver;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.examples.grpc.rest.*;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A sample gRPC service using proto2
 * which has a complementary RESTful interface.
 *
 * @author tkral
 */
@AutoBindSingleton
public class UserGrpcServiceV2 extends UserServiceV2Grpc.UserServiceV2ImplBase {

    private final MapStore mapStore;

    @Inject
    UserGrpcServiceV2(final MapStore mapStore) {
        this.mapStore = mapStore;
    }

    @Override
    public void deleteUserV2(
        final DeleteUserRequestV2 request,
        final StreamObserver<DeleteUserResponseV2> responseObserver
    ) {
        final MapStoreKey userRecordKey = createUserRecordKey(request.getId());
        mapStore.deleteItem("users", userRecordKey);

        responseObserver.onNext(
            DeleteUserResponseV2.newBuilder()
                .setId(request.getId())
                .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void getUserV2(
        final GetUserRequestV2 request,
        final StreamObserver<GetUserResponseV2> responseObserver
    ) {
        final MapStoreKey userRecordKey = createUserRecordKey(request.getId());
        final Optional<Map<String, Object>> userRecordMap = mapStore.getItem("users", userRecordKey);

        final GetUserResponseV2.Builder responseBuilder = GetUserResponseV2.newBuilder();
        userRecordMap.ifPresent(map -> {
            map.entrySet().stream()
                .filter(entry -> GetUserResponseV2.getDescriptor().findFieldByName(entry.getKey()) != null)
                .forEach(entry -> {
                    final FieldDescriptor fieldDescriptor = GetUserResponseV2.getDescriptor().findFieldByName(entry.getKey());
                    responseBuilder.setField(fieldDescriptor, entry.getValue());
                });
        });

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void postUserV2(
        final PostUserRequestV2 request,
        final StreamObserver<PostUserResponseV2> responseObserver
    ) {
        final String userId = UUID.randomUUID().toString();

        final MapStoreKey userRecordKey = createUserRecordKey(userId);
        final UserRecordV2 userRecord = UserRecordV2.newBuilder()
            .setId(userId)
            .setName(request.getName())
            .build();

        final Map<String, Object> userRecordMap = userRecord.getAllFields().entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
        mapStore.putItem("users", userRecordKey, userRecordMap);

        responseObserver.onNext(
            PostUserResponseV2.newBuilder()
                .setId(userId)
                .setName(request.getName())
                .build()
        );
        responseObserver.onCompleted();
    }

    private MapStoreKey createUserRecordKey(final String userId) {
        return new MapStoreKey.Builder()
            .setHash("id", userId)
            .build();
    }
}
