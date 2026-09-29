package pl.photodrive.core.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.photodrive.core.application.service.StudyProgressService;
import pl.photodrive.core.domain.model.StudyProgress;
import pl.photodrive.core.presentation.dto.study.StudyProgressDto;
import pl.photodrive.core.presentation.dto.study.StudyProgressRequest;

@RestController
@RequestMapping("/api/public/study/progress")
@RequiredArgsConstructor
public class StudyProgressController {

    private final StudyProgressService studyProgressService;

    @GetMapping("/{code}")
    public ResponseEntity<StudyProgressDto> get(@PathVariable("code") String code) {
        return noStore(studyProgressService.get(code));
    }

    @PutMapping("/{code}")
    public ResponseEntity<StudyProgressDto> save(@PathVariable("code") String code,
                                                 @RequestBody StudyProgressRequest request) {
        return noStore(studyProgressService.save(code, request.learned()));
    }

    private static ResponseEntity<StudyProgressDto> noStore(StudyProgress progress) {
        Long updatedAt = progress.updatedAt() == null ? null : progress.updatedAt().toEpochMilli();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new StudyProgressDto(progress.learned(), updatedAt));
    }
}
