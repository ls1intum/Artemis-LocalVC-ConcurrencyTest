package de.tum.cit.ase.artemisModel;

public class TemplateProgrammingExerciseParticipation extends Participation {

    private String repositoryUri;
    private String buildPlanId;

    public String getRepositoryUri() {
        return repositoryUri;
    }

    public void setRepositoryUri(String repositoryUri) {
        this.repositoryUri = repositoryUri;
    }

    public String getBuildPlanId() {
        return buildPlanId;
    }

    public void setBuildPlanId(String buildPlanId) {
        this.buildPlanId = buildPlanId;
    }
}
