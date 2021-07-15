package util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target({FIELD,LOCAL_VARIABLE,METHOD,PARAMETER,TYPE})
@Inherited
/**
 * The targets of this annotation are excempt from I/O-related issues described
 * by the {@linkplain Suppresses} constant.
 * 
 * @author AzureTriple
 */
public @interface NoIO {
    /**
     * An enumeration which lists what protections this annotation guarantees.
     * 
     * @see Suppresses#EXCEPTIONS
     * @see Suppresses#RESOURCE_LEAKS
     * @see Suppresses#ALL
     */
    public static enum Suppresses {
        /**Exempts I/O exceptions, security exceptions, etc.*/
        EXCEPTIONS,
        /**Exempts manual closing of resources outside of the heap.*/
        RESOURCE_LEAKS,
        /**Exempts exceptions and resource leaks.*/
        ALL
    }
    /**
     * Defaults to {@linkplain Suppresses#ALL}.
     * 
     * @see Suppresses
     */
    Suppresses suppresses() default Suppresses.ALL;
}