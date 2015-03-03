package alien4cloud.model.components.constraints;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = { "lessThan" })
@SuppressWarnings({ "PMD.UnusedPrivateField", "unchecked" })
public class LessThanConstraint extends AbstractComparablePropertyConstraint {
    @NotNull
    private String lessThan;

    @Override
    public void initialize(ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        initialize(lessThan, propertyType);
    }

    @Override
    protected void doValidate(Object propertyValue) throws ConstraintViolationException {
        if (getComparable().compareTo(propertyValue) <= 0) {
            throw new ConstraintViolationException(propertyValue + " > " + lessThan);
        }
    }

    @Override
    public boolean isCompatible(PropertyConstraint propertyConstraint) {
        if ((propertyConstraint instanceof LessThanConstraint) && this.getLessThan().equals(((LessThanConstraint) propertyConstraint).getLessThan())) {
            return true;
        }
        return false;
    }
}