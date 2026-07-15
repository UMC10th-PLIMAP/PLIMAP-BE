package com.example.plimap.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "member_follow")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberFollow {

    @EmbeddedId
    private MemberFollowId id;

    @MapsId("followerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "follower_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_follow_follower"))
    private Member follower;

    @MapsId("followingId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "following_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_follow_following"))
    private Member following;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private MemberFollow(Member follower, Member following) {
        if (follower == null || follower.getId() == null) {
            throw new IllegalArgumentException("follower must be persisted");
        }
        if (following == null || following.getId() == null) {
            throw new IllegalArgumentException("following must be persisted");
        }

        this.follower = follower;
        this.following = following;
        this.id = new MemberFollowId(follower.getId(), following.getId());
    }

    public static MemberFollow create(Member follower, Member following) {
        return MemberFollow.builder()
                .follower(follower)
                .following(following)
                .build();
    }
}
