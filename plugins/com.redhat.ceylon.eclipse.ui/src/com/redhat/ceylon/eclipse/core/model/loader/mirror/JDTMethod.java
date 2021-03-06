/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.redhat.ceylon.eclipse.core.model.loader.mirror;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodVerifier;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

import com.redhat.ceylon.compiler.loader.mirror.AnnotationMirror;
import com.redhat.ceylon.compiler.loader.mirror.MethodMirror;
import com.redhat.ceylon.compiler.loader.mirror.TypeMirror;
import com.redhat.ceylon.compiler.loader.mirror.TypeParameterMirror;
import com.redhat.ceylon.compiler.loader.mirror.VariableMirror;

public class JDTMethod implements MethodMirror {

    private MethodBinding method;
    private MethodVerifier methodVerifier;
    private Map<String, AnnotationMirror> annotations;
    private String name;
    private List<VariableMirror> parameters;
    private TypeMirror returnType;
    private List<TypeParameterMirror> typeParameters;
    Boolean isOverriding;
    
    public JDTMethod(MethodBinding method, LookupEnvironment lookupEnvironment) {
        this.method = method;
        this.methodVerifier = lookupEnvironment.methodVerifier();
    }

    @Override
    public AnnotationMirror getAnnotation(String type) {
        if (annotations == null) {
            annotations = JDTUtils.getAnnotations(method.getAnnotations());
        }
        return annotations.get(type);
    }

    @Override
    public String getName() {
        if (name == null) {
            if (isConstructor() || isStaticInit()) {
                name = new String(method.declaringClass.sourceName); 
            }
            name = new String(method.selector);
        }
        return name;
    }

    @Override
    public boolean isStatic() {
        return method.isStatic();
    }

    @Override
    public boolean isPublic() {
        return method.isPublic();
    }

    @Override
    public boolean isConstructor() {
        return method.isConstructor();
    }

    @Override
    public boolean isStaticInit() {
        return method.selector == TypeConstants.CLINIT; // TODO : check if it is right
    }

    @Override
    public List<VariableMirror> getParameters() {
        if (parameters == null) {
            TypeBinding[] javaParameters;
            AnnotationBinding[][] annotations;
            javaParameters = ((MethodBinding)method).parameters;
            annotations = ((MethodBinding)method).getParameterAnnotations();
            if (annotations == null) {
                annotations = new AnnotationBinding[javaParameters.length][];
                for (int i=0; i<annotations.length; i++) {
                    annotations[i] = new AnnotationBinding[0];
                }
            }
            parameters = new ArrayList<VariableMirror>(javaParameters.length);
            for(int i=0;i<javaParameters.length;i++)
                parameters.add(new JDTVariable(javaParameters[i], annotations[i]));
        }
        return parameters;
    }

    @Override
    public boolean isAbstract() {
        return method.isAbstract();
    }

    @Override
    public boolean isFinal() {
        return method.isFinal();
    }

    @Override
    public TypeMirror getReturnType() {
        if (returnType == null) {
            returnType = new JDTType(method.returnType);
        }
        return returnType;
    }

    @Override
    public List<TypeParameterMirror> getTypeParameters() {
        if (typeParameters == null) {
            TypeVariableBinding[] jdtTypeParameters = method.typeVariables();
            typeParameters = new ArrayList<TypeParameterMirror>(jdtTypeParameters.length);
            for(TypeVariableBinding jdtTypeParameter : jdtTypeParameters)
                typeParameters.add(new JDTTypeParameter(jdtTypeParameter));
        }
        return typeParameters;
    }

    public boolean isOverridingMethod() {
        if (isOverriding == null) {
            isOverriding = Boolean.FALSE;

            ReferenceBinding declaringClass = method.declaringClass;
            // try the superclass first
            if (isDefinedInSuperClasses(declaringClass)) {
                isOverriding = Boolean.TRUE;
            } 
            if (isDefinedInSuperInterfaces(declaringClass)) {
                isOverriding = Boolean.TRUE;
            }
        }
        return isOverriding.booleanValue();
    }

    private boolean isDefinedInType(ReferenceBinding superClass) {
        for (MethodBinding inheritedMethod : superClass.methods()) {
            if (methodVerifier.doesMethodOverride(method, inheritedMethod)) {
                return true;
            }
        }
        return false;
    }
    
    boolean isDefinedInSuperClasses(ReferenceBinding declaringClass) {
        ReferenceBinding superClass = declaringClass.superclass();
        if (superClass == null) {
            return false;
        }
        if (isDefinedInType(superClass)) {
            return true;
        }
        return isDefinedInSuperClasses(superClass);
    }

    boolean isDefinedInSuperInterfaces(ReferenceBinding declaringType) {
        ReferenceBinding[] superInterfaces = declaringType.superInterfaces();
        if (superInterfaces == null) {
            return false;
        }
        
        for (ReferenceBinding superInterface : superInterfaces) {
            if (isDefinedInType(superInterface)) {
                return true;
            }
            if (isDefinedInSuperInterfaces(superInterface)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isProtected() {
        return method.isProtected();
    }

    @Override
    public boolean isDefaultAccess() {
        return method.isDefault();
    }
}
