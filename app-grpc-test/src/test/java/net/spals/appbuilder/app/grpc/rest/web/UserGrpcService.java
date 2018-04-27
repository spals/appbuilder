package net.spals.appbuilder.app.grpc.rest.web;

import com.google.inject.Inject;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.grpc.stub.StreamObserver;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.grpc.rest.*;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A sample gRPC service which has a complementary RESTful interface.
 *
 * @author tkral
 */
@AutoBindSingleton
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final MapStore mapStore;

    @Inject
    UserGrpcService(final MapStore mapStore) {
        this.mapStore = mapStore;
    }

    @Override
    public void deleteUser(
        final DeleteUserRequest request,
        final StreamObserver<DeleteUserResponse> responseObserver
    ) {
        final MapStoreKey userRecordKey = createUserRecordKey(request.getId());
        mapStore.deleteItem("users", userRecordKey);

        responseObserver.onNext(
            DeleteUserResponse.newBuilder()
                .setId(request.getId())
                .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void getUser(
        final GetUserRequest request,
        final StreamObserver<GetUserResponse> responseObserver
    ) {
        final MapStoreKey userRecordKey = createUserRecordKey(request.getId());
        final Optional<Map<String, Object>> userRecordMap = mapStore.getItem("users", userRecordKey);

        final GetUserResponse.Builder responseBuilder = GetUserResponse.newBuilder();
        userRecordMap.ifPresent(map -> {
            map.entrySet().stream()
                .filter(entry -> GetUserResponse.getDescriptor().findFieldByName(entry.getKey()) != null)
                .forEach(entry -> {
                    final FieldDescriptor fieldDescriptor = GetUserResponse.getDescriptor().findFieldByName(entry.getKey());
                    responseBuilder.setField(fieldDescriptor, entry.getValue());
                });
        });

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void postUser(
        final PostUserRequest request,
        final StreamObserver<PostUserResponse> responseObserver
    ) {
        final String userId = UUID.randomUUID().toString();

        final MapStoreKey userRecordKey = createUserRecordKey(userId);
        final UserRecord userRecord = UserRecord.newBuilder()
            .setId(userId)
            .setName(request.getName())
            .build();

        final Map<String, Object> userRecordMap = userRecord.getAllFields().entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
        mapStore.putItem("users", userRecordKey, userRecordMap);

        responseObserver.onNext(
            PostUserResponse.newBuilder()
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
