package com.esprit.planning.service;

import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.repository.ProgressUpdateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningScheduledJobsTest {

    @Mock
    private ProgressUpdateRepository progressUpdateRepository;

    @Mock
    private PlanningNotificationService planningNotificationService;

    @InjectMocks
    private PlanningScheduledJobs planningScheduledJobs;

    private ProgressUpdate overdue;

    @BeforeEach
    void setUp() {
        overdue = ProgressUpdate.builder()
                .id(10L)
                .projectId(1L)
                .freelancerId(99L)
                .title("Sprint 1")
                .description("desc")
                .progressPercentage(50)
                .nextUpdateDue(LocalDateTime.now().minusDays(1))
                .nextDueOverdueNotified(false)
                .build();
    }

    @Test
    void notifyOverdueNextProgressDue_marksNotifiedAndNotifies() {
        when(progressUpdateRepository.findByNextUpdateDueIsNotNullAndNextUpdateDueBeforeAndNextDueOverdueNotifiedIsFalse(any()))
                .thenReturn(List.of(overdue));

        planningScheduledJobs.notifyOverdueNextProgressDue();

        ArgumentCaptor<ProgressUpdate> saved = ArgumentCaptor.forClass(ProgressUpdate.class);
        verify(progressUpdateRepository).save(saved.capture());
        assertThat(saved.getValue().getNextDueOverdueNotified()).isTrue();

        verify(planningNotificationService).notifyUser(
                eq("99"),
                eq("Progress update due date passed"),
                org.mockito.ArgumentMatchers.contains("Sprint 1"),
                eq(PlanningNotificationService.TYPE_PROGRESS_NEXT_DUE_OVERDUE),
                org.mockito.ArgumentMatchers.argThat((Map<String, String> m) ->
                        "1".equals(m.get("projectId")) && "10".equals(m.get("progressUpdateId"))));
    }

    @Test
    void clearOrphanNextDueCalendarEventIds_delegatesToRepository() {
        when(progressUpdateRepository.clearOrphanNextDueCalendarEventIds()).thenReturn(2);

        planningScheduledJobs.clearOrphanNextDueCalendarEventIds();

        verify(progressUpdateRepository).clearOrphanNextDueCalendarEventIds();
    }
}
