package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList)) {
                        return new PsiReference[0];
                    }

                    ParameterList parameterList = (ParameterList) psiElement.getContext();
                    PsiElement methodReference = parameterList.getContext();

                    int domainParameter = -1;
                    if (methodReference instanceof MethodReference && PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, TranslationUtil.PHP_TRANSLATION_SIGNATURES)) {
                        domainParameter = 2;
                        if("transChoice".equals(((MethodReference) methodReference).getName())) {
                            domainParameter = 3;
                        }
                    } else if(methodReference instanceof NewExpression && PhpElementsUtil.isNewExpressionPhpClassWithInstance((NewExpression) methodReference, TranslationUtil.PHP_TRANSLATION_TRANSLATABLE_MESSAGE)) {
                        domainParameter = 2;
                    }

                    if (domainParameter < 0) {
                        return new PsiReference[0];
                    }

                    ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
                    if(currentIndex == null) {
                        return new PsiReference[0];
                    }

                    if(currentIndex.getIndex() == domainParameter) {
                        return new PsiReference[]{ new TranslationDomainReference((StringLiteralExpression) psiElement) };
                    }

                    if(currentIndex.getIndex() == 0) {
                        String domain = PsiElementUtils.getMethodParameterAt(parameterList, domainParameter);

                        if(domain == null) {
                            domain = "messages";
                        }

                        return new PsiReference[]{ new TranslationReference((StringLiteralExpression) psiElement, domain) };
                    }

                    return new PsiReference[0];
                }

            }

        );

    }

}
