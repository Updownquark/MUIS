// Generated from src/main/java/org/quick/core/parser/attr/QuickAttr.g4 by ANTLR 4.5.3
package org.quick.core.parser.attr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link QuickAttrParser}.
 */
public interface QuickAttrListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(QuickAttrParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(QuickAttrParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(QuickAttrParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(QuickAttrParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(QuickAttrParser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(QuickAttrParser.PrimitiveTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#numericType}.
	 * @param ctx the parse tree
	 */
	void enterNumericType(QuickAttrParser.NumericTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#numericType}.
	 * @param ctx the parse tree
	 */
	void exitNumericType(QuickAttrParser.NumericTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#integralType}.
	 * @param ctx the parse tree
	 */
	void enterIntegralType(QuickAttrParser.IntegralTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#integralType}.
	 * @param ctx the parse tree
	 */
	void exitIntegralType(QuickAttrParser.IntegralTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#floatingPointType}.
	 * @param ctx the parse tree
	 */
	void enterFloatingPointType(QuickAttrParser.FloatingPointTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#floatingPointType}.
	 * @param ctx the parse tree
	 */
	void exitFloatingPointType(QuickAttrParser.FloatingPointTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#referenceType}.
	 * @param ctx the parse tree
	 */
	void enterReferenceType(QuickAttrParser.ReferenceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#referenceType}.
	 * @param ctx the parse tree
	 */
	void exitReferenceType(QuickAttrParser.ReferenceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceType(QuickAttrParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceType(QuickAttrParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classType}.
	 * @param ctx the parse tree
	 */
	void enterClassType(QuickAttrParser.ClassTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classType}.
	 * @param ctx the parse tree
	 */
	void exitClassType(QuickAttrParser.ClassTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classType_lf_classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassType_lf_classOrInterfaceType(QuickAttrParser.ClassType_lf_classOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classType_lf_classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassType_lf_classOrInterfaceType(QuickAttrParser.ClassType_lf_classOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classType_lfno_classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassType_lfno_classOrInterfaceType(QuickAttrParser.ClassType_lfno_classOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classType_lfno_classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassType_lfno_classOrInterfaceType(QuickAttrParser.ClassType_lfno_classOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceType}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceType(QuickAttrParser.InterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceType}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceType(QuickAttrParser.InterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceType_lf_classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceType_lf_classOrInterfaceType(QuickAttrParser.InterfaceType_lf_classOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceType_lf_classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceType_lf_classOrInterfaceType(QuickAttrParser.InterfaceType_lf_classOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceType_lfno_classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceType_lfno_classOrInterfaceType(QuickAttrParser.InterfaceType_lfno_classOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceType_lfno_classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceType_lfno_classOrInterfaceType(QuickAttrParser.InterfaceType_lfno_classOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeVariable}.
	 * @param ctx the parse tree
	 */
	void enterTypeVariable(QuickAttrParser.TypeVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeVariable}.
	 * @param ctx the parse tree
	 */
	void exitTypeVariable(QuickAttrParser.TypeVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#arrayType}.
	 * @param ctx the parse tree
	 */
	void enterArrayType(QuickAttrParser.ArrayTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#arrayType}.
	 * @param ctx the parse tree
	 */
	void exitArrayType(QuickAttrParser.ArrayTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#dims}.
	 * @param ctx the parse tree
	 */
	void enterDims(QuickAttrParser.DimsContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#dims}.
	 * @param ctx the parse tree
	 */
	void exitDims(QuickAttrParser.DimsContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(QuickAttrParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(QuickAttrParser.TypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeParameterModifier}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameterModifier(QuickAttrParser.TypeParameterModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeParameterModifier}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameterModifier(QuickAttrParser.TypeParameterModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void enterTypeBound(QuickAttrParser.TypeBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void exitTypeBound(QuickAttrParser.TypeBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#additionalBound}.
	 * @param ctx the parse tree
	 */
	void enterAdditionalBound(QuickAttrParser.AdditionalBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#additionalBound}.
	 * @param ctx the parse tree
	 */
	void exitAdditionalBound(QuickAttrParser.AdditionalBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(QuickAttrParser.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(QuickAttrParser.TypeArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeArgumentList}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgumentList(QuickAttrParser.TypeArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeArgumentList}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgumentList(QuickAttrParser.TypeArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgument(QuickAttrParser.TypeArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgument(QuickAttrParser.TypeArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#wildcard}.
	 * @param ctx the parse tree
	 */
	void enterWildcard(QuickAttrParser.WildcardContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#wildcard}.
	 * @param ctx the parse tree
	 */
	void exitWildcard(QuickAttrParser.WildcardContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#wildcardBounds}.
	 * @param ctx the parse tree
	 */
	void enterWildcardBounds(QuickAttrParser.WildcardBoundsContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#wildcardBounds}.
	 * @param ctx the parse tree
	 */
	void exitWildcardBounds(QuickAttrParser.WildcardBoundsContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#packageName}.
	 * @param ctx the parse tree
	 */
	void enterPackageName(QuickAttrParser.PackageNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#packageName}.
	 * @param ctx the parse tree
	 */
	void exitPackageName(QuickAttrParser.PackageNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterTypeName(QuickAttrParser.TypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitTypeName(QuickAttrParser.TypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#packageOrTypeName}.
	 * @param ctx the parse tree
	 */
	void enterPackageOrTypeName(QuickAttrParser.PackageOrTypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#packageOrTypeName}.
	 * @param ctx the parse tree
	 */
	void exitPackageOrTypeName(QuickAttrParser.PackageOrTypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#expressionName}.
	 * @param ctx the parse tree
	 */
	void enterExpressionName(QuickAttrParser.ExpressionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#expressionName}.
	 * @param ctx the parse tree
	 */
	void exitExpressionName(QuickAttrParser.ExpressionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodName}.
	 * @param ctx the parse tree
	 */
	void enterMethodName(QuickAttrParser.MethodNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodName}.
	 * @param ctx the parse tree
	 */
	void exitMethodName(QuickAttrParser.MethodNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#ambiguousName}.
	 * @param ctx the parse tree
	 */
	void enterAmbiguousName(QuickAttrParser.AmbiguousNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#ambiguousName}.
	 * @param ctx the parse tree
	 */
	void exitAmbiguousName(QuickAttrParser.AmbiguousNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(QuickAttrParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(QuickAttrParser.CompilationUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPackageDeclaration(QuickAttrParser.PackageDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPackageDeclaration(QuickAttrParser.PackageDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#packageModifier}.
	 * @param ctx the parse tree
	 */
	void enterPackageModifier(QuickAttrParser.PackageModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#packageModifier}.
	 * @param ctx the parse tree
	 */
	void exitPackageModifier(QuickAttrParser.PackageModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterImportDeclaration(QuickAttrParser.ImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitImportDeclaration(QuickAttrParser.ImportDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#singleTypeImportDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterSingleTypeImportDeclaration(QuickAttrParser.SingleTypeImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#singleTypeImportDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitSingleTypeImportDeclaration(QuickAttrParser.SingleTypeImportDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeImportOnDemandDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeImportOnDemandDeclaration(QuickAttrParser.TypeImportOnDemandDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeImportOnDemandDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeImportOnDemandDeclaration(QuickAttrParser.TypeImportOnDemandDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#singleStaticImportDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterSingleStaticImportDeclaration(QuickAttrParser.SingleStaticImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#singleStaticImportDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitSingleStaticImportDeclaration(QuickAttrParser.SingleStaticImportDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#staticImportOnDemandDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterStaticImportOnDemandDeclaration(QuickAttrParser.StaticImportOnDemandDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#staticImportOnDemandDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitStaticImportOnDemandDeclaration(QuickAttrParser.StaticImportOnDemandDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeDeclaration(QuickAttrParser.TypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeDeclaration(QuickAttrParser.TypeDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassDeclaration(QuickAttrParser.ClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassDeclaration(QuickAttrParser.ClassDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#normalClassDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterNormalClassDeclaration(QuickAttrParser.NormalClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#normalClassDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitNormalClassDeclaration(QuickAttrParser.NormalClassDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classModifier}.
	 * @param ctx the parse tree
	 */
	void enterClassModifier(QuickAttrParser.ClassModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classModifier}.
	 * @param ctx the parse tree
	 */
	void exitClassModifier(QuickAttrParser.ClassModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(QuickAttrParser.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(QuickAttrParser.TypeParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeParameterList}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameterList(QuickAttrParser.TypeParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeParameterList}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameterList(QuickAttrParser.TypeParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#superclass}.
	 * @param ctx the parse tree
	 */
	void enterSuperclass(QuickAttrParser.SuperclassContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#superclass}.
	 * @param ctx the parse tree
	 */
	void exitSuperclass(QuickAttrParser.SuperclassContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#superinterfaces}.
	 * @param ctx the parse tree
	 */
	void enterSuperinterfaces(QuickAttrParser.SuperinterfacesContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#superinterfaces}.
	 * @param ctx the parse tree
	 */
	void exitSuperinterfaces(QuickAttrParser.SuperinterfacesContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceTypeList}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceTypeList(QuickAttrParser.InterfaceTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceTypeList}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceTypeList(QuickAttrParser.InterfaceTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(QuickAttrParser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(QuickAttrParser.ClassBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassBodyDeclaration(QuickAttrParser.ClassBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassBodyDeclaration(QuickAttrParser.ClassBodyDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassMemberDeclaration(QuickAttrParser.ClassMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassMemberDeclaration(QuickAttrParser.ClassMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFieldDeclaration(QuickAttrParser.FieldDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFieldDeclaration(QuickAttrParser.FieldDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#fieldModifier}.
	 * @param ctx the parse tree
	 */
	void enterFieldModifier(QuickAttrParser.FieldModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#fieldModifier}.
	 * @param ctx the parse tree
	 */
	void exitFieldModifier(QuickAttrParser.FieldModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#variableDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaratorList(QuickAttrParser.VariableDeclaratorListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#variableDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaratorList(QuickAttrParser.VariableDeclaratorListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarator(QuickAttrParser.VariableDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarator(QuickAttrParser.VariableDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaratorId(QuickAttrParser.VariableDeclaratorIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaratorId(QuickAttrParser.VariableDeclaratorIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void enterVariableInitializer(QuickAttrParser.VariableInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void exitVariableInitializer(QuickAttrParser.VariableInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannType}.
	 * @param ctx the parse tree
	 */
	void enterUnannType(QuickAttrParser.UnannTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannType}.
	 * @param ctx the parse tree
	 */
	void exitUnannType(QuickAttrParser.UnannTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannPrimitiveType}.
	 * @param ctx the parse tree
	 */
	void enterUnannPrimitiveType(QuickAttrParser.UnannPrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannPrimitiveType}.
	 * @param ctx the parse tree
	 */
	void exitUnannPrimitiveType(QuickAttrParser.UnannPrimitiveTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannReferenceType}.
	 * @param ctx the parse tree
	 */
	void enterUnannReferenceType(QuickAttrParser.UnannReferenceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannReferenceType}.
	 * @param ctx the parse tree
	 */
	void exitUnannReferenceType(QuickAttrParser.UnannReferenceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterUnannClassOrInterfaceType(QuickAttrParser.UnannClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitUnannClassOrInterfaceType(QuickAttrParser.UnannClassOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannClassType}.
	 * @param ctx the parse tree
	 */
	void enterUnannClassType(QuickAttrParser.UnannClassTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannClassType}.
	 * @param ctx the parse tree
	 */
	void exitUnannClassType(QuickAttrParser.UnannClassTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannClassType_lf_unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterUnannClassType_lf_unannClassOrInterfaceType(QuickAttrParser.UnannClassType_lf_unannClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannClassType_lf_unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitUnannClassType_lf_unannClassOrInterfaceType(QuickAttrParser.UnannClassType_lf_unannClassOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannClassType_lfno_unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterUnannClassType_lfno_unannClassOrInterfaceType(QuickAttrParser.UnannClassType_lfno_unannClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannClassType_lfno_unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitUnannClassType_lfno_unannClassOrInterfaceType(QuickAttrParser.UnannClassType_lfno_unannClassOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterUnannInterfaceType(QuickAttrParser.UnannInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitUnannInterfaceType(QuickAttrParser.UnannInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannInterfaceType_lf_unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterUnannInterfaceType_lf_unannClassOrInterfaceType(QuickAttrParser.UnannInterfaceType_lf_unannClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannInterfaceType_lf_unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitUnannInterfaceType_lf_unannClassOrInterfaceType(QuickAttrParser.UnannInterfaceType_lf_unannClassOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannInterfaceType_lfno_unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterUnannInterfaceType_lfno_unannClassOrInterfaceType(QuickAttrParser.UnannInterfaceType_lfno_unannClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannInterfaceType_lfno_unannClassOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitUnannInterfaceType_lfno_unannClassOrInterfaceType(QuickAttrParser.UnannInterfaceType_lfno_unannClassOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannTypeVariable}.
	 * @param ctx the parse tree
	 */
	void enterUnannTypeVariable(QuickAttrParser.UnannTypeVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannTypeVariable}.
	 * @param ctx the parse tree
	 */
	void exitUnannTypeVariable(QuickAttrParser.UnannTypeVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unannArrayType}.
	 * @param ctx the parse tree
	 */
	void enterUnannArrayType(QuickAttrParser.UnannArrayTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unannArrayType}.
	 * @param ctx the parse tree
	 */
	void exitUnannArrayType(QuickAttrParser.UnannArrayTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(QuickAttrParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(QuickAttrParser.MethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodModifier}.
	 * @param ctx the parse tree
	 */
	void enterMethodModifier(QuickAttrParser.MethodModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodModifier}.
	 * @param ctx the parse tree
	 */
	void exitMethodModifier(QuickAttrParser.MethodModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodHeader}.
	 * @param ctx the parse tree
	 */
	void enterMethodHeader(QuickAttrParser.MethodHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodHeader}.
	 * @param ctx the parse tree
	 */
	void exitMethodHeader(QuickAttrParser.MethodHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#result}.
	 * @param ctx the parse tree
	 */
	void enterResult(QuickAttrParser.ResultContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#result}.
	 * @param ctx the parse tree
	 */
	void exitResult(QuickAttrParser.ResultContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclarator(QuickAttrParser.MethodDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclarator(QuickAttrParser.MethodDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameterList(QuickAttrParser.FormalParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameterList(QuickAttrParser.FormalParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameters(QuickAttrParser.FormalParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameters(QuickAttrParser.FormalParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameter(QuickAttrParser.FormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameter(QuickAttrParser.FormalParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void enterVariableModifier(QuickAttrParser.VariableModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void exitVariableModifier(QuickAttrParser.VariableModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void enterLastFormalParameter(QuickAttrParser.LastFormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void exitLastFormalParameter(QuickAttrParser.LastFormalParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#receiverParameter}.
	 * @param ctx the parse tree
	 */
	void enterReceiverParameter(QuickAttrParser.ReceiverParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#receiverParameter}.
	 * @param ctx the parse tree
	 */
	void exitReceiverParameter(QuickAttrParser.ReceiverParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#throws_}.
	 * @param ctx the parse tree
	 */
	void enterThrows_(QuickAttrParser.Throws_Context ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#throws_}.
	 * @param ctx the parse tree
	 */
	void exitThrows_(QuickAttrParser.Throws_Context ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#exceptionTypeList}.
	 * @param ctx the parse tree
	 */
	void enterExceptionTypeList(QuickAttrParser.ExceptionTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#exceptionTypeList}.
	 * @param ctx the parse tree
	 */
	void exitExceptionTypeList(QuickAttrParser.ExceptionTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#exceptionType}.
	 * @param ctx the parse tree
	 */
	void enterExceptionType(QuickAttrParser.ExceptionTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#exceptionType}.
	 * @param ctx the parse tree
	 */
	void exitExceptionType(QuickAttrParser.ExceptionTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void enterMethodBody(QuickAttrParser.MethodBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void exitMethodBody(QuickAttrParser.MethodBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#instanceInitializer}.
	 * @param ctx the parse tree
	 */
	void enterInstanceInitializer(QuickAttrParser.InstanceInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#instanceInitializer}.
	 * @param ctx the parse tree
	 */
	void exitInstanceInitializer(QuickAttrParser.InstanceInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#staticInitializer}.
	 * @param ctx the parse tree
	 */
	void enterStaticInitializer(QuickAttrParser.StaticInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#staticInitializer}.
	 * @param ctx the parse tree
	 */
	void exitStaticInitializer(QuickAttrParser.StaticInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructorDeclaration(QuickAttrParser.ConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructorDeclaration(QuickAttrParser.ConstructorDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#constructorModifier}.
	 * @param ctx the parse tree
	 */
	void enterConstructorModifier(QuickAttrParser.ConstructorModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#constructorModifier}.
	 * @param ctx the parse tree
	 */
	void exitConstructorModifier(QuickAttrParser.ConstructorModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#constructorDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterConstructorDeclarator(QuickAttrParser.ConstructorDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#constructorDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitConstructorDeclarator(QuickAttrParser.ConstructorDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#simpleTypeName}.
	 * @param ctx the parse tree
	 */
	void enterSimpleTypeName(QuickAttrParser.SimpleTypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#simpleTypeName}.
	 * @param ctx the parse tree
	 */
	void exitSimpleTypeName(QuickAttrParser.SimpleTypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void enterConstructorBody(QuickAttrParser.ConstructorBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void exitConstructorBody(QuickAttrParser.ConstructorBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#explicitConstructorInvocation}.
	 * @param ctx the parse tree
	 */
	void enterExplicitConstructorInvocation(QuickAttrParser.ExplicitConstructorInvocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#explicitConstructorInvocation}.
	 * @param ctx the parse tree
	 */
	void exitExplicitConstructorInvocation(QuickAttrParser.ExplicitConstructorInvocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterEnumDeclaration(QuickAttrParser.EnumDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitEnumDeclaration(QuickAttrParser.EnumDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#enumBody}.
	 * @param ctx the parse tree
	 */
	void enterEnumBody(QuickAttrParser.EnumBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#enumBody}.
	 * @param ctx the parse tree
	 */
	void exitEnumBody(QuickAttrParser.EnumBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#enumConstantList}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstantList(QuickAttrParser.EnumConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#enumConstantList}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstantList(QuickAttrParser.EnumConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstant(QuickAttrParser.EnumConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstant(QuickAttrParser.EnumConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#enumConstantModifier}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstantModifier(QuickAttrParser.EnumConstantModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#enumConstantModifier}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstantModifier(QuickAttrParser.EnumConstantModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void enterEnumBodyDeclarations(QuickAttrParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void exitEnumBodyDeclarations(QuickAttrParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceDeclaration(QuickAttrParser.InterfaceDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceDeclaration(QuickAttrParser.InterfaceDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#normalInterfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterNormalInterfaceDeclaration(QuickAttrParser.NormalInterfaceDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#normalInterfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitNormalInterfaceDeclaration(QuickAttrParser.NormalInterfaceDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceModifier}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceModifier(QuickAttrParser.InterfaceModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceModifier}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceModifier(QuickAttrParser.InterfaceModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#extendsInterfaces}.
	 * @param ctx the parse tree
	 */
	void enterExtendsInterfaces(QuickAttrParser.ExtendsInterfacesContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#extendsInterfaces}.
	 * @param ctx the parse tree
	 */
	void exitExtendsInterfaces(QuickAttrParser.ExtendsInterfacesContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBody(QuickAttrParser.InterfaceBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBody(QuickAttrParser.InterfaceBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMemberDeclaration(QuickAttrParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMemberDeclaration(QuickAttrParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#constantDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstantDeclaration(QuickAttrParser.ConstantDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#constantDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstantDeclaration(QuickAttrParser.ConstantDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#constantModifier}.
	 * @param ctx the parse tree
	 */
	void enterConstantModifier(QuickAttrParser.ConstantModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#constantModifier}.
	 * @param ctx the parse tree
	 */
	void exitConstantModifier(QuickAttrParser.ConstantModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMethodDeclaration(QuickAttrParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMethodDeclaration(QuickAttrParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#interfaceMethodModifier}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMethodModifier(QuickAttrParser.InterfaceMethodModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#interfaceMethodModifier}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMethodModifier(QuickAttrParser.InterfaceMethodModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeDeclaration(QuickAttrParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeDeclaration(QuickAttrParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeBody(QuickAttrParser.AnnotationTypeBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeBody(QuickAttrParser.AnnotationTypeBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#annotationTypeMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeMemberDeclaration(QuickAttrParser.AnnotationTypeMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#annotationTypeMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeMemberDeclaration(QuickAttrParser.AnnotationTypeMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementDeclaration(QuickAttrParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementDeclaration(QuickAttrParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#annotationTypeElementModifier}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementModifier(QuickAttrParser.AnnotationTypeElementModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#annotationTypeElementModifier}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementModifier(QuickAttrParser.AnnotationTypeElementModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(QuickAttrParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(QuickAttrParser.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(QuickAttrParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(QuickAttrParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#normalAnnotation}.
	 * @param ctx the parse tree
	 */
	void enterNormalAnnotation(QuickAttrParser.NormalAnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#normalAnnotation}.
	 * @param ctx the parse tree
	 */
	void exitNormalAnnotation(QuickAttrParser.NormalAnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#elementValuePairList}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairList(QuickAttrParser.ElementValuePairListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#elementValuePairList}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairList(QuickAttrParser.ElementValuePairListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePair(QuickAttrParser.ElementValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePair(QuickAttrParser.ElementValuePairContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void enterElementValue(QuickAttrParser.ElementValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void exitElementValue(QuickAttrParser.ElementValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterElementValueArrayInitializer(QuickAttrParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitElementValueArrayInitializer(QuickAttrParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#elementValueList}.
	 * @param ctx the parse tree
	 */
	void enterElementValueList(QuickAttrParser.ElementValueListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#elementValueList}.
	 * @param ctx the parse tree
	 */
	void exitElementValueList(QuickAttrParser.ElementValueListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#markerAnnotation}.
	 * @param ctx the parse tree
	 */
	void enterMarkerAnnotation(QuickAttrParser.MarkerAnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#markerAnnotation}.
	 * @param ctx the parse tree
	 */
	void exitMarkerAnnotation(QuickAttrParser.MarkerAnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#singleElementAnnotation}.
	 * @param ctx the parse tree
	 */
	void enterSingleElementAnnotation(QuickAttrParser.SingleElementAnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#singleElementAnnotation}.
	 * @param ctx the parse tree
	 */
	void exitSingleElementAnnotation(QuickAttrParser.SingleElementAnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterArrayInitializer(QuickAttrParser.ArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitArrayInitializer(QuickAttrParser.ArrayInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#variableInitializerList}.
	 * @param ctx the parse tree
	 */
	void enterVariableInitializerList(QuickAttrParser.VariableInitializerListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#variableInitializerList}.
	 * @param ctx the parse tree
	 */
	void exitVariableInitializerList(QuickAttrParser.VariableInitializerListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(QuickAttrParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(QuickAttrParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#blockStatements}.
	 * @param ctx the parse tree
	 */
	void enterBlockStatements(QuickAttrParser.BlockStatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#blockStatements}.
	 * @param ctx the parse tree
	 */
	void exitBlockStatements(QuickAttrParser.BlockStatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void enterBlockStatement(QuickAttrParser.BlockStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void exitBlockStatement(QuickAttrParser.BlockStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclarationStatement(QuickAttrParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclarationStatement(QuickAttrParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclaration(QuickAttrParser.LocalVariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclaration(QuickAttrParser.LocalVariableDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(QuickAttrParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(QuickAttrParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#statementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void enterStatementNoShortIf(QuickAttrParser.StatementNoShortIfContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#statementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void exitStatementNoShortIf(QuickAttrParser.StatementNoShortIfContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#statementWithoutTrailingSubstatement}.
	 * @param ctx the parse tree
	 */
	void enterStatementWithoutTrailingSubstatement(QuickAttrParser.StatementWithoutTrailingSubstatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#statementWithoutTrailingSubstatement}.
	 * @param ctx the parse tree
	 */
	void exitStatementWithoutTrailingSubstatement(QuickAttrParser.StatementWithoutTrailingSubstatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStatement(QuickAttrParser.EmptyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStatement(QuickAttrParser.EmptyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#labeledStatement}.
	 * @param ctx the parse tree
	 */
	void enterLabeledStatement(QuickAttrParser.LabeledStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#labeledStatement}.
	 * @param ctx the parse tree
	 */
	void exitLabeledStatement(QuickAttrParser.LabeledStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#labeledStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void enterLabeledStatementNoShortIf(QuickAttrParser.LabeledStatementNoShortIfContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#labeledStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void exitLabeledStatementNoShortIf(QuickAttrParser.LabeledStatementNoShortIfContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(QuickAttrParser.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(QuickAttrParser.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void enterStatementExpression(QuickAttrParser.StatementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void exitStatementExpression(QuickAttrParser.StatementExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#ifThenStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfThenStatement(QuickAttrParser.IfThenStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#ifThenStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfThenStatement(QuickAttrParser.IfThenStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#ifThenElseStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfThenElseStatement(QuickAttrParser.IfThenElseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#ifThenElseStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfThenElseStatement(QuickAttrParser.IfThenElseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#ifThenElseStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void enterIfThenElseStatementNoShortIf(QuickAttrParser.IfThenElseStatementNoShortIfContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#ifThenElseStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void exitIfThenElseStatementNoShortIf(QuickAttrParser.IfThenElseStatementNoShortIfContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#assertStatement}.
	 * @param ctx the parse tree
	 */
	void enterAssertStatement(QuickAttrParser.AssertStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#assertStatement}.
	 * @param ctx the parse tree
	 */
	void exitAssertStatement(QuickAttrParser.AssertStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#switchStatement}.
	 * @param ctx the parse tree
	 */
	void enterSwitchStatement(QuickAttrParser.SwitchStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#switchStatement}.
	 * @param ctx the parse tree
	 */
	void exitSwitchStatement(QuickAttrParser.SwitchStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#switchBlock}.
	 * @param ctx the parse tree
	 */
	void enterSwitchBlock(QuickAttrParser.SwitchBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#switchBlock}.
	 * @param ctx the parse tree
	 */
	void exitSwitchBlock(QuickAttrParser.SwitchBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void enterSwitchBlockStatementGroup(QuickAttrParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void exitSwitchBlockStatementGroup(QuickAttrParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#switchLabels}.
	 * @param ctx the parse tree
	 */
	void enterSwitchLabels(QuickAttrParser.SwitchLabelsContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#switchLabels}.
	 * @param ctx the parse tree
	 */
	void exitSwitchLabels(QuickAttrParser.SwitchLabelsContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void enterSwitchLabel(QuickAttrParser.SwitchLabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void exitSwitchLabel(QuickAttrParser.SwitchLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstantName(QuickAttrParser.EnumConstantNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstantName(QuickAttrParser.EnumConstantNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(QuickAttrParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(QuickAttrParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#whileStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatementNoShortIf(QuickAttrParser.WhileStatementNoShortIfContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#whileStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatementNoShortIf(QuickAttrParser.WhileStatementNoShortIfContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#doStatement}.
	 * @param ctx the parse tree
	 */
	void enterDoStatement(QuickAttrParser.DoStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#doStatement}.
	 * @param ctx the parse tree
	 */
	void exitDoStatement(QuickAttrParser.DoStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStatement(QuickAttrParser.ForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStatement(QuickAttrParser.ForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#forStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void enterForStatementNoShortIf(QuickAttrParser.ForStatementNoShortIfContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#forStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void exitForStatementNoShortIf(QuickAttrParser.ForStatementNoShortIfContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#basicForStatement}.
	 * @param ctx the parse tree
	 */
	void enterBasicForStatement(QuickAttrParser.BasicForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#basicForStatement}.
	 * @param ctx the parse tree
	 */
	void exitBasicForStatement(QuickAttrParser.BasicForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#basicForStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void enterBasicForStatementNoShortIf(QuickAttrParser.BasicForStatementNoShortIfContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#basicForStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void exitBasicForStatementNoShortIf(QuickAttrParser.BasicForStatementNoShortIfContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#forInit}.
	 * @param ctx the parse tree
	 */
	void enterForInit(QuickAttrParser.ForInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#forInit}.
	 * @param ctx the parse tree
	 */
	void exitForInit(QuickAttrParser.ForInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void enterForUpdate(QuickAttrParser.ForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void exitForUpdate(QuickAttrParser.ForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#statementExpressionList}.
	 * @param ctx the parse tree
	 */
	void enterStatementExpressionList(QuickAttrParser.StatementExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#statementExpressionList}.
	 * @param ctx the parse tree
	 */
	void exitStatementExpressionList(QuickAttrParser.StatementExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#enhancedForStatement}.
	 * @param ctx the parse tree
	 */
	void enterEnhancedForStatement(QuickAttrParser.EnhancedForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#enhancedForStatement}.
	 * @param ctx the parse tree
	 */
	void exitEnhancedForStatement(QuickAttrParser.EnhancedForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#enhancedForStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void enterEnhancedForStatementNoShortIf(QuickAttrParser.EnhancedForStatementNoShortIfContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#enhancedForStatementNoShortIf}.
	 * @param ctx the parse tree
	 */
	void exitEnhancedForStatementNoShortIf(QuickAttrParser.EnhancedForStatementNoShortIfContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void enterBreakStatement(QuickAttrParser.BreakStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void exitBreakStatement(QuickAttrParser.BreakStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void enterContinueStatement(QuickAttrParser.ContinueStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void exitContinueStatement(QuickAttrParser.ContinueStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(QuickAttrParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(QuickAttrParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#throwStatement}.
	 * @param ctx the parse tree
	 */
	void enterThrowStatement(QuickAttrParser.ThrowStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#throwStatement}.
	 * @param ctx the parse tree
	 */
	void exitThrowStatement(QuickAttrParser.ThrowStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#synchronizedStatement}.
	 * @param ctx the parse tree
	 */
	void enterSynchronizedStatement(QuickAttrParser.SynchronizedStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#synchronizedStatement}.
	 * @param ctx the parse tree
	 */
	void exitSynchronizedStatement(QuickAttrParser.SynchronizedStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#tryStatement}.
	 * @param ctx the parse tree
	 */
	void enterTryStatement(QuickAttrParser.TryStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#tryStatement}.
	 * @param ctx the parse tree
	 */
	void exitTryStatement(QuickAttrParser.TryStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#catches}.
	 * @param ctx the parse tree
	 */
	void enterCatches(QuickAttrParser.CatchesContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#catches}.
	 * @param ctx the parse tree
	 */
	void exitCatches(QuickAttrParser.CatchesContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void enterCatchClause(QuickAttrParser.CatchClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void exitCatchClause(QuickAttrParser.CatchClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#catchFormalParameter}.
	 * @param ctx the parse tree
	 */
	void enterCatchFormalParameter(QuickAttrParser.CatchFormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#catchFormalParameter}.
	 * @param ctx the parse tree
	 */
	void exitCatchFormalParameter(QuickAttrParser.CatchFormalParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#catchType}.
	 * @param ctx the parse tree
	 */
	void enterCatchType(QuickAttrParser.CatchTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#catchType}.
	 * @param ctx the parse tree
	 */
	void exitCatchType(QuickAttrParser.CatchTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#finally_}.
	 * @param ctx the parse tree
	 */
	void enterFinally_(QuickAttrParser.Finally_Context ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#finally_}.
	 * @param ctx the parse tree
	 */
	void exitFinally_(QuickAttrParser.Finally_Context ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#tryWithResourcesStatement}.
	 * @param ctx the parse tree
	 */
	void enterTryWithResourcesStatement(QuickAttrParser.TryWithResourcesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#tryWithResourcesStatement}.
	 * @param ctx the parse tree
	 */
	void exitTryWithResourcesStatement(QuickAttrParser.TryWithResourcesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void enterResourceSpecification(QuickAttrParser.ResourceSpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void exitResourceSpecification(QuickAttrParser.ResourceSpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#resourceList}.
	 * @param ctx the parse tree
	 */
	void enterResourceList(QuickAttrParser.ResourceListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#resourceList}.
	 * @param ctx the parse tree
	 */
	void exitResourceList(QuickAttrParser.ResourceListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(QuickAttrParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(QuickAttrParser.ResourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(QuickAttrParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(QuickAttrParser.PrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primaryNoNewArray}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryNoNewArray(QuickAttrParser.PrimaryNoNewArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primaryNoNewArray}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryNoNewArray(QuickAttrParser.PrimaryNoNewArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lf_arrayAccess}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryNoNewArray_lf_arrayAccess(QuickAttrParser.PrimaryNoNewArray_lf_arrayAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lf_arrayAccess}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryNoNewArray_lf_arrayAccess(QuickAttrParser.PrimaryNoNewArray_lf_arrayAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lfno_arrayAccess}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryNoNewArray_lfno_arrayAccess(QuickAttrParser.PrimaryNoNewArray_lfno_arrayAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lfno_arrayAccess}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryNoNewArray_lfno_arrayAccess(QuickAttrParser.PrimaryNoNewArray_lfno_arrayAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lf_primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryNoNewArray_lf_primary(QuickAttrParser.PrimaryNoNewArray_lf_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lf_primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryNoNewArray_lf_primary(QuickAttrParser.PrimaryNoNewArray_lf_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary(QuickAttrParser.PrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primary(QuickAttrParser.PrimaryNoNewArray_lf_primary_lf_arrayAccess_lf_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary(QuickAttrParser.PrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primary(QuickAttrParser.PrimaryNoNewArray_lf_primary_lfno_arrayAccess_lf_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryNoNewArray_lfno_primary(QuickAttrParser.PrimaryNoNewArray_lfno_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryNoNewArray_lfno_primary(QuickAttrParser.PrimaryNoNewArray_lfno_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary(QuickAttrParser.PrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primary(QuickAttrParser.PrimaryNoNewArray_lfno_primary_lf_arrayAccess_lfno_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(QuickAttrParser.PrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#primaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(QuickAttrParser.PrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classInstanceCreationExpression}.
	 * @param ctx the parse tree
	 */
	void enterClassInstanceCreationExpression(QuickAttrParser.ClassInstanceCreationExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classInstanceCreationExpression}.
	 * @param ctx the parse tree
	 */
	void exitClassInstanceCreationExpression(QuickAttrParser.ClassInstanceCreationExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classInstanceCreationExpression_lf_primary}.
	 * @param ctx the parse tree
	 */
	void enterClassInstanceCreationExpression_lf_primary(QuickAttrParser.ClassInstanceCreationExpression_lf_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classInstanceCreationExpression_lf_primary}.
	 * @param ctx the parse tree
	 */
	void exitClassInstanceCreationExpression_lf_primary(QuickAttrParser.ClassInstanceCreationExpression_lf_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#classInstanceCreationExpression_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void enterClassInstanceCreationExpression_lfno_primary(QuickAttrParser.ClassInstanceCreationExpression_lfno_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#classInstanceCreationExpression_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void exitClassInstanceCreationExpression_lfno_primary(QuickAttrParser.ClassInstanceCreationExpression_lfno_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgumentsOrDiamond(QuickAttrParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgumentsOrDiamond(QuickAttrParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#fieldAccess}.
	 * @param ctx the parse tree
	 */
	void enterFieldAccess(QuickAttrParser.FieldAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#fieldAccess}.
	 * @param ctx the parse tree
	 */
	void exitFieldAccess(QuickAttrParser.FieldAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#fieldAccess_lf_primary}.
	 * @param ctx the parse tree
	 */
	void enterFieldAccess_lf_primary(QuickAttrParser.FieldAccess_lf_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#fieldAccess_lf_primary}.
	 * @param ctx the parse tree
	 */
	void exitFieldAccess_lf_primary(QuickAttrParser.FieldAccess_lf_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#fieldAccess_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void enterFieldAccess_lfno_primary(QuickAttrParser.FieldAccess_lfno_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#fieldAccess_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void exitFieldAccess_lfno_primary(QuickAttrParser.FieldAccess_lfno_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#arrayAccess}.
	 * @param ctx the parse tree
	 */
	void enterArrayAccess(QuickAttrParser.ArrayAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#arrayAccess}.
	 * @param ctx the parse tree
	 */
	void exitArrayAccess(QuickAttrParser.ArrayAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#arrayAccess_lf_primary}.
	 * @param ctx the parse tree
	 */
	void enterArrayAccess_lf_primary(QuickAttrParser.ArrayAccess_lf_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#arrayAccess_lf_primary}.
	 * @param ctx the parse tree
	 */
	void exitArrayAccess_lf_primary(QuickAttrParser.ArrayAccess_lf_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#arrayAccess_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void enterArrayAccess_lfno_primary(QuickAttrParser.ArrayAccess_lfno_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#arrayAccess_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void exitArrayAccess_lfno_primary(QuickAttrParser.ArrayAccess_lfno_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodInvocation}.
	 * @param ctx the parse tree
	 */
	void enterMethodInvocation(QuickAttrParser.MethodInvocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodInvocation}.
	 * @param ctx the parse tree
	 */
	void exitMethodInvocation(QuickAttrParser.MethodInvocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodInvocation_lf_primary}.
	 * @param ctx the parse tree
	 */
	void enterMethodInvocation_lf_primary(QuickAttrParser.MethodInvocation_lf_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodInvocation_lf_primary}.
	 * @param ctx the parse tree
	 */
	void exitMethodInvocation_lf_primary(QuickAttrParser.MethodInvocation_lf_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodInvocation_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void enterMethodInvocation_lfno_primary(QuickAttrParser.MethodInvocation_lfno_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodInvocation_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void exitMethodInvocation_lfno_primary(QuickAttrParser.MethodInvocation_lfno_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(QuickAttrParser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(QuickAttrParser.ArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodReference}.
	 * @param ctx the parse tree
	 */
	void enterMethodReference(QuickAttrParser.MethodReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodReference}.
	 * @param ctx the parse tree
	 */
	void exitMethodReference(QuickAttrParser.MethodReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodReference_lf_primary}.
	 * @param ctx the parse tree
	 */
	void enterMethodReference_lf_primary(QuickAttrParser.MethodReference_lf_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodReference_lf_primary}.
	 * @param ctx the parse tree
	 */
	void exitMethodReference_lf_primary(QuickAttrParser.MethodReference_lf_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#methodReference_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void enterMethodReference_lfno_primary(QuickAttrParser.MethodReference_lfno_primaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#methodReference_lfno_primary}.
	 * @param ctx the parse tree
	 */
	void exitMethodReference_lfno_primary(QuickAttrParser.MethodReference_lfno_primaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#arrayCreationExpression}.
	 * @param ctx the parse tree
	 */
	void enterArrayCreationExpression(QuickAttrParser.ArrayCreationExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#arrayCreationExpression}.
	 * @param ctx the parse tree
	 */
	void exitArrayCreationExpression(QuickAttrParser.ArrayCreationExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#dimExprs}.
	 * @param ctx the parse tree
	 */
	void enterDimExprs(QuickAttrParser.DimExprsContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#dimExprs}.
	 * @param ctx the parse tree
	 */
	void exitDimExprs(QuickAttrParser.DimExprsContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#dimExpr}.
	 * @param ctx the parse tree
	 */
	void enterDimExpr(QuickAttrParser.DimExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#dimExpr}.
	 * @param ctx the parse tree
	 */
	void exitDimExpr(QuickAttrParser.DimExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpression(QuickAttrParser.ConstantExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpression(QuickAttrParser.ConstantExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(QuickAttrParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(QuickAttrParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#lambdaExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambdaExpression(QuickAttrParser.LambdaExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#lambdaExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambdaExpression(QuickAttrParser.LambdaExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#lambdaParameters}.
	 * @param ctx the parse tree
	 */
	void enterLambdaParameters(QuickAttrParser.LambdaParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#lambdaParameters}.
	 * @param ctx the parse tree
	 */
	void exitLambdaParameters(QuickAttrParser.LambdaParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#inferredFormalParameterList}.
	 * @param ctx the parse tree
	 */
	void enterInferredFormalParameterList(QuickAttrParser.InferredFormalParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#inferredFormalParameterList}.
	 * @param ctx the parse tree
	 */
	void exitInferredFormalParameterList(QuickAttrParser.InferredFormalParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#lambdaBody}.
	 * @param ctx the parse tree
	 */
	void enterLambdaBody(QuickAttrParser.LambdaBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#lambdaBody}.
	 * @param ctx the parse tree
	 */
	void exitLambdaBody(QuickAttrParser.LambdaBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#assignmentExpression}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentExpression(QuickAttrParser.AssignmentExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#assignmentExpression}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentExpression(QuickAttrParser.AssignmentExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(QuickAttrParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(QuickAttrParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unitExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnitExpression(QuickAttrParser.UnitExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unitExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnitExpression(QuickAttrParser.UnitExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#leftHandSide}.
	 * @param ctx the parse tree
	 */
	void enterLeftHandSide(QuickAttrParser.LeftHandSideContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#leftHandSide}.
	 * @param ctx the parse tree
	 */
	void exitLeftHandSide(QuickAttrParser.LeftHandSideContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentOperator(QuickAttrParser.AssignmentOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentOperator(QuickAttrParser.AssignmentOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#conditionalExpression}.
	 * @param ctx the parse tree
	 */
	void enterConditionalExpression(QuickAttrParser.ConditionalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#conditionalExpression}.
	 * @param ctx the parse tree
	 */
	void exitConditionalExpression(QuickAttrParser.ConditionalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#conditionalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterConditionalOrExpression(QuickAttrParser.ConditionalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#conditionalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitConditionalOrExpression(QuickAttrParser.ConditionalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#conditionalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterConditionalAndExpression(QuickAttrParser.ConditionalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#conditionalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitConditionalAndExpression(QuickAttrParser.ConditionalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#inclusiveOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterInclusiveOrExpression(QuickAttrParser.InclusiveOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#inclusiveOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitInclusiveOrExpression(QuickAttrParser.InclusiveOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#exclusiveOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterExclusiveOrExpression(QuickAttrParser.ExclusiveOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#exclusiveOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitExclusiveOrExpression(QuickAttrParser.ExclusiveOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#andExpression}.
	 * @param ctx the parse tree
	 */
	void enterAndExpression(QuickAttrParser.AndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#andExpression}.
	 * @param ctx the parse tree
	 */
	void exitAndExpression(QuickAttrParser.AndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpression(QuickAttrParser.EqualityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpression(QuickAttrParser.EqualityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(QuickAttrParser.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(QuickAttrParser.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#shiftExpression}.
	 * @param ctx the parse tree
	 */
	void enterShiftExpression(QuickAttrParser.ShiftExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#shiftExpression}.
	 * @param ctx the parse tree
	 */
	void exitShiftExpression(QuickAttrParser.ShiftExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(QuickAttrParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(QuickAttrParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(QuickAttrParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(QuickAttrParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(QuickAttrParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(QuickAttrParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#preIncrementExpression}.
	 * @param ctx the parse tree
	 */
	void enterPreIncrementExpression(QuickAttrParser.PreIncrementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#preIncrementExpression}.
	 * @param ctx the parse tree
	 */
	void exitPreIncrementExpression(QuickAttrParser.PreIncrementExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#preDecrementExpression}.
	 * @param ctx the parse tree
	 */
	void enterPreDecrementExpression(QuickAttrParser.PreDecrementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#preDecrementExpression}.
	 * @param ctx the parse tree
	 */
	void exitPreDecrementExpression(QuickAttrParser.PreDecrementExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#unaryExpressionNotPlusMinus}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpressionNotPlusMinus(QuickAttrParser.UnaryExpressionNotPlusMinusContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#unaryExpressionNotPlusMinus}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpressionNotPlusMinus(QuickAttrParser.UnaryExpressionNotPlusMinusContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExpression(QuickAttrParser.PostfixExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExpression(QuickAttrParser.PostfixExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#postIncrementExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostIncrementExpression(QuickAttrParser.PostIncrementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#postIncrementExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostIncrementExpression(QuickAttrParser.PostIncrementExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#postIncrementExpression_lf_postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostIncrementExpression_lf_postfixExpression(QuickAttrParser.PostIncrementExpression_lf_postfixExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#postIncrementExpression_lf_postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostIncrementExpression_lf_postfixExpression(QuickAttrParser.PostIncrementExpression_lf_postfixExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#postDecrementExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostDecrementExpression(QuickAttrParser.PostDecrementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#postDecrementExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostDecrementExpression(QuickAttrParser.PostDecrementExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#postDecrementExpression_lf_postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostDecrementExpression_lf_postfixExpression(QuickAttrParser.PostDecrementExpression_lf_postfixExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#postDecrementExpression_lf_postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostDecrementExpression_lf_postfixExpression(QuickAttrParser.PostDecrementExpression_lf_postfixExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link QuickAttrParser#castExpression}.
	 * @param ctx the parse tree
	 */
	void enterCastExpression(QuickAttrParser.CastExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link QuickAttrParser#castExpression}.
	 * @param ctx the parse tree
	 */
	void exitCastExpression(QuickAttrParser.CastExpressionContext ctx);
}