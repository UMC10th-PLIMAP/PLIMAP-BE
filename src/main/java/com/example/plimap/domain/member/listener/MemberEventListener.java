package com.example.plimap.domain.member.listener;

import com.example.plimap.domain.member.event.MemberWithdrawnEvent;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import com.example.plimap.global.external.storage.ProfileImageStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberEventListener {

    private final ProfileImageStorage profileImageStorage;

    // 탈퇴 트랜잭션 커밋 이후에만 스토리지 객체를 지운다. 커밋 전에 지우면 커밋 실패(롤백) 시
    // DB는 여전히 기존 objectKey를 가리키는데 실제 객체는 이미 삭제된 상태가 되어버린다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberWithdrawn(MemberWithdrawnEvent event) {
        String objectKey = event.profileImageObjectKey();
        if (objectKey == null) {
            return;
        }
        try {
            profileImageStorage.delete(objectKey);
        } catch (ProfileImageStorageException e) {
            log.warn("탈퇴 처리 중 프로필 이미지 삭제 실패: objectKey={}", objectKey, e);
        }
    }
}
