package fi.vm.yti.datamodel.api.v2.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DataModelValidator.class)
public @interface ValidDatamodel {

    String message() default "Invalid data";

    Class<?>[] groups() default {};

    boolean updateModel() default false;
    Class<? extends Payload>[] payload() default {};
}
