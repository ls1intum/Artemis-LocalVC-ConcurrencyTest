package de.tum.cit.ase.artemisModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.HashSet;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
// Annotation necessary to distinguish between concrete implementations of Exercise when deserializing from JSON
// @formatter:off
@JsonSubTypes({
        @JsonSubTypes.Type(value = ProgrammingExercise.class, name = "programming"),
        @JsonSubTypes.Type(value = ModelingExercise.class, name = "modeling"),
        @JsonSubTypes.Type(value = QuizExercise.class, name = "quiz"),
        @JsonSubTypes.Type(value = TextExercise.class, name = "text"),
//        @JsonSubTypes.Type(value = FileUploadExercise.class, name = "file-upload")
})
// @formatter:on
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class Exercise extends DomainObject {

    private String problemStatement;
    private ExerciseGroup exerciseGroup;
    private String title;
    private Double maxPoints;
    private Course course;

    @JsonIgnoreProperties("exercise")
    private Set<StudentParticipation> studentParticipations = new HashSet<>();

    public String getProblemStatement() {
        return problemStatement;
    }

    public void setProblemStatement(String problemStatement) {
        this.problemStatement = problemStatement;
    }

    public Set<StudentParticipation> getStudentParticipations() {
        return studentParticipations;
    }

    public void setStudentParticipations(Set<StudentParticipation> studentParticipations) {
        this.studentParticipations = studentParticipations;
    }

    public ExerciseGroup getExerciseGroup() {
        return exerciseGroup;
    }

    public void setExerciseGroup(ExerciseGroup exerciseGroup) {
        this.exerciseGroup = exerciseGroup;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(Double maxPoints) {
        this.maxPoints = maxPoints;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
