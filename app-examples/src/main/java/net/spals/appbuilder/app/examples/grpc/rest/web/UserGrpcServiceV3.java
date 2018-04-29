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
 * A sample gRPC service using proto3
 * which has a complementary RESTful interface.
 *
 * @author tkral
 */
@AutoBindSingleton
public class UserGrpcServiceV3 extends UserServiceV3Grpc.UserServiceV3ImplBase {

    private final MapStore mapStore;

    @Inject
    UserGrpcServiceV3(final MapStore mapStore) {
        this.mapStore = mapStore;
    }

    @Override
    public void deleteUserV3(
        final DeleteUserRequestV3 request,
        final StreamObserver<DeleteUserResponseV3> responseObserver
    ) {
        final MapStoreKey userRecordKey = createUserRecordKey(request.getId());
        mapStore.deleteItem("users", userRecordKey);

        responseObserver.onNext(
            DeleteUserResponseV3.newBuilder()
                .setId(request.getId())
                .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void getUserV3(
        final GetUserRequestV3 request,
        final StreamObserver<GetUserResponseV3> responseObserver
    ) {
        final MapStoreKey userRecordKey = createUserRecordKey(request.getId());
        final Optional<Map<String, Object>> userRecordMap = mapStore.getItem("users", userRecordKey);

        final GetUserResponseV3.Builder responseBuilder = GetUserResponseV3.newBuilder();
        userRecordMap.ifPresent(map -> {
            map.entrySet().stream()
                .filter(entry -> GetUserResponseV3.getDescriptor().findFieldByName(entry.getKey()) != null)
                .forEach(entry -> {
                    final FieldDescriptor fieldDescriptor = GetUserResponseV3.getDescriptor().findFieldByName(entry.getKey());
                    responseBuilder.setField(fieldDescriptor, entry.getValue());
                });
        });

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void postUserV3(
        final PostUserRequestV3 request,
        final StreamObserver<PostUserResponseV3> responseObserver
    ) {
        final String userId = UUID.randomUUID().toString();

        final MapStoreKey userRecordKey = createUserRecordKey(userId);
        final UserRecordV3 userRecord = UserRecordV3.newBuilder()
            .setId(userId)
            .setName(request.getName())
            .build();

        final Map<String, Object> userRecordMap = userRecord.getAllFields().entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
        mapStore.putItem("users", userRecordKey, userRecordMap);

        responseObserver.onNext(
            PostUserResponseV3.newBuilder()
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
