package com.mindrevol.backend.modules.journey.dto.request;

import com.mindrevol.backend.modules.journey.entity.InteractionType;
import com.mindrevol.backend.modules.journey.entity.JourneyType;
import com.mindrevol.backend.modules.journey.entity.JourneyVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateJourneyRequest {
    @NotBlank(message = "Tên hành trình không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Loại hành trình là bắt buộc")
    private JourneyType type;

    // Các thiết lập về thời gian
    private LocalDate startDate;
    private LocalDate endDate;

    // Các thiết lập về giao diện & hiển thị
    private String theme; // Lưu mã màu hex (vd: #FF5733) hoặc tên theme

    // Các thiết lập về quyền riêng tư & tương tác (Đã bổ sung)
    @NotNull(message = "Quyền riêng tư là bắt buộc")
    private JourneyVisibility visibility;

    @NotNull(message = "Loại tương tác là bắt buộc")
    private InteractionType interactionType;

    // Danh sách task nếu là loại ROADMAP (Optional)
    private List<JourneyTaskRequest> roadmapTasks;
}