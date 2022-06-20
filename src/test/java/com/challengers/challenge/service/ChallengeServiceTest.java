package com.challengers.challenge.service;

import com.challengers.challenge.domain.Challenge;
import com.challengers.challenge.dto.ChallengeRequest;
import com.challengers.challenge.repository.ChallengeRepository;
import com.challengers.challenge.service.ChallengeService;
import com.challengers.common.AwsS3Uploader;
import com.challengers.examplephoto.repository.ExamplePhotoRepository;
import com.challengers.tag.domain.Tag;
import com.challengers.tag.repository.TagRepository;
import com.challengers.user.domain.AuthProvider;
import com.challengers.user.domain.Role;
import com.challengers.user.domain.User;
import com.challengers.user.repository.UserRepository;
import com.challengers.userchallenge.domain.UserChallenge;
import com.challengers.userchallenge.repository.UserChallengeRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChallengeServiceTest {
    @Mock UserRepository userRepository;
    @Mock ChallengeRepository challengeRepository;
    @Mock ExamplePhotoRepository examplePhotoRepository;
    @Mock TagRepository tagRepository;
    @Mock UserChallengeRepository userChallengeRepository;
    @Mock AwsS3Uploader awsS3Uploader;

    ChallengeService challengeService;

    ChallengeRequest challengeRequest;
    User user;

    @BeforeEach
    void setUp() {
        challengeService = new ChallengeService(challengeRepository,tagRepository,
                userRepository,examplePhotoRepository,userChallengeRepository,awsS3Uploader);

        user = User.builder()
                .id(0L)
                .name("테스터")
                .email("test@gmail.com")
                .image("https:/asfawfasfas.png")
                .bio("my bio")
                .password(null)
                .role(Role.USER)
                .provider(AuthProvider.google)
                .providerId("12412521")
                .build();

        challengeRequest = ChallengeRequest.builder()
                .challengeName("미라클 모닝 - 아침 7시 기상")
                .image(new MockMultipartFile("테스트사진.png","테스트사진.png","image/png","saf".getBytes()))
                .challengePhotoDescription("7시를 가르키는 시계와 본인이 같이 나오게 사진을 찍으시면 됩니다.")
                .checkFrequency("EVERY_DAY")
                .category("LIFE")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now())
                .depositPoint(1000)
                .introduction("매일 아침 7시에 일어나면 하루가 개운합니다.")
                .examplePhotos(new ArrayList<>(Arrays.asList(
                        new MockMultipartFile("예시사진1.png","예시사진1.png","image/png","asgas".getBytes()),
                        new MockMultipartFile("예시사진2.png","예시사진2.png","image/png","asgasagagas".getBytes())
                )))
                .tags(new ArrayList<>(Arrays.asList("미라클모닝", "기상")))
                .build();
    }

    @Test
    @DisplayName("챌린지 개설 성공")
    void create() {
        //given
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(awsS3Uploader.uploadImage(any()))
                .thenReturn("https://challengers-bucket.s3.ap-northeast-2.amazonaws.com/1747f32c-e5083c5e2bce0.PNG");
        when(tagRepository.findTagByName(any())).thenReturn(Optional.of(new Tag("임시 태그")));

        //when
        challengeService.create(challengeRequest, user.getId());

        //then
        verify(challengeRepository).save(any());
    }

    @Test
    @DisplayName("챌린지 삭제 성공")
    void delete() {
        Challenge challenge = Challenge.builder().host(user).build();
        when(challengeRepository.findById(any())).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.countByChallengeId(any())).thenReturn(1L);
        when(userChallengeRepository.findByUserIdAndChallengeId(any(),any())).thenReturn(Optional.of(new UserChallenge(challenge,user,false)));

        challengeService.delete(challenge.getId(),user.getId());

        verify(challengeRepository).delete(any());
    }


    @Test
    @DisplayName("챌린지 삭제 실패 - 참가자가 2명 이상일 경우")
    void delete_fail_proceeding() {
        Challenge challenge = Challenge.builder().host(user).build();
        when(challengeRepository.findById(any())).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.countByChallengeId(any())).thenReturn(2L);

        assertThatThrownBy(() -> challengeService.delete(challenge.getId(),user.getId()))
                .isInstanceOf(RuntimeException.class);
    }
}