package fi.vm.yti.datamodel.api.v2.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

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
@Constraint(validatedBy = ClassValidator.class)
public @interface ValidClass {

    String message() default "Invalid data";

    Class<?>[] groups() default {};

    boolean updateClass() default false;
    Class<? extends Payload>[] payload() default {};
}
