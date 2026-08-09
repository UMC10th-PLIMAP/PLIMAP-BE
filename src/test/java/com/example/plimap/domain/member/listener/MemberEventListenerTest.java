package com.example.plimap.domain.member.listener;

import com.example.plimap.domain.member.event.MemberWithdrawnEvent;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import com.example.plimap.global.external.storage.ProfileImageStorageException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MemberEventListenerTest {

    private final ProfileImageStorage profileImageStorage = mock(ProfileImageStorage.class);
    private final MemberEventListener listener = new MemberEventListener(profileImageStorage);

    @Test
    void 탈퇴_이벤트에_프로필_이미지_키가_있으면_스토리지에서_삭제한다() {
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, "old-key");

        listener.handleMemberWithdrawn(event);

        verify(profileImageStorage).delete("old-key");
    }

    @Test
    void 탈퇴_이벤트에_프로필_이미지_키가_없으면_스토리지를_호출하지_않는다() {
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, null);

        listener.handleMemberWithdrawn(event);

        verify(profileImageStorage, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 스토리지_삭제가_실패해도_예외를_전파하지_않는다() {
        doThrow(new ProfileImageStorageException("삭제 실패", new RuntimeException()))
                .when(profileImageStorage).delete("old-key");
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, "old-key");

        assertThatCode(() -> listener.handleMemberWithdrawn(event)).doesNotThrowAnyException();
    }
}
