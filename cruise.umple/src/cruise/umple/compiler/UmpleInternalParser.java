/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.13.0.605 modeling language!*/

package cruise.umple.compiler;
import java.io.*;
import cruise.umple.util.*;
import java.util.*;

public class UmpleInternalParser extends Parser implements UmpleParser
{

  //------------------------
  // MEMBER VARIABLES
  //------------------------

  //UmpleInternalParser Attributes
  private String currentPackageName;
  private UmpleModel model;
  private List<String> unparsedUmpleFiles;
  private List<String> parsedUmpleFiles;
  private List<AssociationVariable> unlinkedAssociationVariables;
  private List<Association> unlinkedAssociations;
  private Map<Position,String> positionToClassNameReference;
  private Map<UmpleClass,List<String>> unlinkedExtends;
  private Map<UmpleInterface,List<String>> unlinkedInterfaceExtends;
  private StateMachine placeholderStateMachine;
  private Map<String,Token> stateMachineNameToToken;
  private Map<UmpleClass,Pair> umpleClassToStateMachineDefinition;

  //------------------------
  // CONSTRUCTOR
  //------------------------

  public UmpleInternalParser(String aName, UmpleModel aModel)
  {
    super(aName);
    currentPackageName = "";
    model = aModel;
    unparsedUmpleFiles = new ArrayList<String>();
    parsedUmpleFiles = new ArrayList<String>();
    unlinkedAssociationVariables = new ArrayList<AssociationVariable>();
    unlinkedAssociations = new ArrayList<Association>();
    positionToClassNameReference = new HashMap<Position, String>();
    unlinkedExtends = new HashMap<UmpleClass,List<String>>();
    unlinkedInterfaceExtends = new HashMap<UmpleInterface,List<String>>();
    placeholderStateMachine = null;
    stateMachineNameToToken = new HashMap<String, Token>();
    umpleClassToStateMachineDefinition = new HashMap<UmpleClass, Pair>();
    init();
  }

  //------------------------
  // INTERFACE
  //------------------------

  public boolean setCurrentPackageName(String aCurrentPackageName)
  {
    boolean wasSet = false;
    currentPackageName = aCurrentPackageName;
    wasSet = true;
    return wasSet;
  }

  public boolean setModel(UmpleModel aModel)
  {
    boolean wasSet = false;
    model = aModel;
    wasSet = true;
    return wasSet;
  }

  public String getCurrentPackageName()
  {
    return currentPackageName;
  }

  public UmpleModel getModel()
  {
    return model;
  }

  public void delete()
  {
    super.delete();
  }

  //------------------------
  // DEVELOPER CODE - PROVIDED AS-IS
  //------------------------
  
   public UmpleInternalParser()  {
this("UmpleInternalParser",new UmpleModel(null));
  }
 public UmpleInternalParser(UmpleModel aModel)  {
this("UmpleInternalParser",aModel);
  }
private void init()
  {
    addCouple(new Couple("\"","\""));
    addCouple(new Couple("{","}"));
    addRulesInFile("/umple_core.grammar");
    addRulesInFile("/umple_classes.grammar");
    addRulesInFile("/umple_patterns.grammar");
    addRulesInFile("/umple_state_machines.grammar");
    addRulesInFile("/umple_traces.grammar");
    addRulesInFile("/umple_layout.grammar");
  }

  public ParseResult parse(String ruleName, String input)
  {
    return super.parse(ruleName,input);
  }

  public ParseResult analyze(boolean shouldGenerate)
  {
    parseAllFiles();
    analyzeAllTokens(getRootToken());
    postTokenAnalysis();
    if (shouldGenerate && getParseResult().getWasSuccess())
    {
      model.generate();
    }
    return getParseResult();
  }

  //------------------------
  // PRIVATE METHODS
  //------------------------

  // When an error occurs, set the failed position and mark the compile as NOT successful
  private void setFailedPosition(Position position)
  {
    getParseResult().setWasSuccess(false);
    getParseResult().setPosition(position);
  }

  // Analyze all child tokens of the "root" token.  This delegates to a individual
  // analyzeToken and quits early if a problem arises
  private void analyzeAllTokens(Token rootToken)
  {
    for(Token t : rootToken.getSubTokens())
    {
      analyzeToken(t);
      if (!getParseResult().getWasSuccess())
      {
        return;
      }
    }
  }

  // Delegate function to analyze a token within the context of a class
  // Each token is analyzed twice, analysisStep is "1" is for the first round of analysis
  // and "2" for the second.  The "2" is used for chicken-and-egg initialization problems, otherwise
  // put everything under the "1"
  private void analyzeAllTokens(Token rootToken, UmpleClass aClass, int analysisStep)
  {
    for(Token token : rootToken.getSubTokens())
    {
      analyzeToken(token,aClass,analysisStep);
      if (!getParseResult().getWasSuccess())
      {
        break;
      }
    }
  }

  // Delegate function to analyze a token and send it to the write
  private void analyzeToken(Token t)
  {
    analyzeCoreToken(t);
    analyzeClassToken(t);
    analyzeStateMachineToken(t);
    analyzeTraceToken(t);
    analyzeLayoutToken(t);
  }

  // Analyze an individual token, delegates to the various components in Umple
  private void analyzeToken(Token t, UmpleClass aClass, int analysisStep)
  {
    analyzeCoreToken(t,aClass,analysisStep);
    analyzeClassToken(t,aClass,analysisStep);
    analyzeStateMachineToken(t,aClass,analysisStep);
    analyzeTraceToken(t,aClass,analysisStep);
    analyzeLayoutToken(t,aClass,analysisStep);
  }
  
  // Once you have analyze all tokens, you allowed a second 'pass' to apply any additional checks
  // Each step in the process might "fail", so we check the status before calling each delegate
  // token post token analysis method
  private void postTokenAnalysis()
  {
    
    if (getParseResult().getWasSuccess())
    {
      postTokenCoreAnalysis();
    }
    if (getParseResult().getWasSuccess())
    {
      postTokenInterfaceAnalysis();
    }
    if (getParseResult().getWasSuccess())
    {
      postTokenClassAnalysis();  
    }

    if (getParseResult().getWasSuccess())
    {
      postTokenStateMachineAnalysis();
    }
    
    if (getParseResult().getWasSuccess())
    {
      postTokenTraceAnalysis();  
    }

    if (getParseResult().getWasSuccess())
    {
      postTokenLayoutAnalysis();
    }

  }


  // Locate all 'use *.ump' references and add those files if not already parsed 
  private void addNecessaryFiles()
  {
    for(Token t : getRootToken().getSubTokens())
    {
      if (t.is("use"))
      {
        String filename = model.getUmpleFile().getPath() + File.separator + t.getValue();

        if (!parsedUmpleFiles.contains(filename) && !unparsedUmpleFiles.contains(filename))
        {
          unparsedUmpleFiles.add(filename);
        }  
      }
    }
  }
  
  // Loop through all unparsed files, parse them, and add any missing references
  private void parseAllFiles()
  {
    addNecessaryFiles();
    while (!unparsedUmpleFiles.isEmpty())
    {
      String nextFile = unparsedUmpleFiles.get(0);
      unparsedUmpleFiles.remove(0);
      parsedUmpleFiles.add(nextFile);
      String input = SampleFileWriter.readContent(new File(nextFile));
      parse("program", input);
      addNecessaryFiles();
    }    
  }
private void analyzeCoreToken(Token t)
  {
    if (t.is("generate"))
    {
      model.addGenerate(t.getValue());
    }
    else if (t.is("glossary"))
    {
      analyzeGlossary(t);
    }
    else if (t.is("namespace"))
    {
      currentPackageName = t.getValue();
      if (model.getDefaultNamespace() == null)
      {
        model.setDefaultNamespace(currentPackageName);  
      }
    }
  }

  // There are currently no core tokens of concern in the context of an UmpleClass
  // This method is available if needed
  private void analyzeCoreToken(Token t, UmpleClass aClass, int analysisStep)
  {

  }

  // Perform post token analysis on core elements of the Umple language
  private void postTokenCoreAnalysis()
  {
    if (model.getDefaultGenerate() == null)
    {
      model.addGenerate("Java");
    }
  }  


  // Add singular / plural forms of words to the glossary to be used by the code generator
  private void analyzeGlossary(Token glossaryToken)
  {
    for(Token wordToken : glossaryToken.getSubTokens())
    {
      if (!wordToken.is("word"))
      {
        continue;
      }
      Word word = new Word(wordToken.getValue("singular"),wordToken.getValue("plural"));
      model.getGlossary().addWord(word);
    }
  }
private void analyzeClassToken(Token t)
  {
    if (t.is("classDefinition"))
    {
      analyzeClass(t);
    }
    else if (t.is("externalDefinition"))
    {
      analyzeExternal(t);
    }
    else if (t.is("interfaceDefinition"))
    {
      analyzeInterface(t);
    }
    else if (t.is("associationClassDefinition"))
    {
      analyzeAssociationClass(t);
    }
    else if (t.is("associationDefinition"))
    {
      analyzeAllAssociations(t);
    }
  }  
  
  // Analyzed class content tokens
  private void analyzeClassToken(Token token, UmpleClass aClass, int analysisStep)
  {
    if (analysisStep != 1)
    {
      return;
    }
    if (token.is("classDefinition"))
    {
      UmpleClass childClass = analyzeClass(token);
      childClass.setExtendsClass(aClass);
    }
    else if (token.is("attribute"))
    {
      analyzeAttribute(token,aClass);
    }
    else if (token.is("beforeCode") || token.is("afterCode"))
    {
      analyzeInjectionCode(token,aClass);
    }
    else if (token.is("key") || token.is("defaultKey")) 
    {
      analyzeKey(token,aClass);
    }
    else if (token.is("extraCode"))
    {
      aClass.appendExtraCode(token.getValue());
    }
    else if (token.is("constantDeclaration"))
    {
      analyzeConstant(token,aClass);
    }
    else if (token.is("concreteMethodDeclaration"))
    {
      analyzeMethod(token,aClass);
    }
    else if (token.is("depend"))
    {
      Depend d = new Depend(token.getValue());
      aClass.addDepend(d);
    }
    else if (token.is("inlineAssociation"))
    {
      analyzeinlineAssociation(token,aClass);
    }
    else if (token.is("symmetricReflexiveAssociation"))
    {
      analyzeSymmetricReflexiveAssociation(token,aClass);
    }
  }  
  
  // Link associations, association variables and extends that were "defined" after their use
  private void postTokenClassAnalysis()
  {
    if (verifyClassesInUse())
    {
      addUnlinkedAssociationVariables();
      addUnlinkedAssociations();
      addUnlinkedExtends();
    }
  }
  
  private void postTokenInterfaceAnalysis()
  {
      addUnlinkedInterfaceExtends();
  }
  
  
  private void analyzeAllAssociations(Token associationToken)
  {
    String name = associationToken.getValue("name");
    for(Token token : associationToken.getSubTokens())
    {
      if (token.is("association"))
      {
        Association association = analyzeAssociation(token, "");
        association.setName(name);
        unlinkedAssociations.add(association);
      }
    }
  }  

  private UmpleClass analyzeClass(Token classToken)
  {
    UmpleClass aClass = model.addUmpleClass(classToken.getValue("name"));
    
    addExtendsTo(classToken, aClass);
    if (classToken.getValue("singleton") != null)
    {
      aClass.setIsSingleton(true);
    }
    aClass.setPackageName(currentPackageName);

    analyzeAllTokens(classToken,aClass,1);
    analyzeAllTokens(classToken,aClass,2);
    return aClass;
  }
  
  private void addExtendsTo(Token classToken, UmpleClass aClass)
  {
    List<String> extendsList = new ArrayList<String>();
    for (Token extendsToken : classToken.getSubTokens()){
      if (extendsToken.getValue("extendsName") != null)
      { 
        extendsList.add(extendsToken.getValue("extendsName"));
        unlinkedExtends.put(aClass, extendsList);
      }  
    }
  }
    
  private void addExtendsTo(Token classToken, UmpleInterface aInterface)
  {
    List<String> extendsList = new ArrayList<String>();
    for (Token extendsToken : classToken.getSubTokens()){
      if (extendsToken.getValue("extendsName") != null)
      { 
        extendsList.add(extendsToken.getValue("extendsName"));
        unlinkedInterfaceExtends.put(aInterface, extendsList);
      }  
    }
  }

  private UmpleClass analyzeExternal(Token externalToken)
  {
    UmpleClass aClass = analyzeClass(externalToken);
    aClass.setModifier("external");
    return aClass;
  }
  
  private void analyzeInterface(Token t)
  {
    UmpleInterface newInterface = new UmpleInterface(t.getValue("name"));
    model.addUmpleInterface(newInterface);
    newInterface.setPackageName(currentPackageName);
    analyzeInterface(t,newInterface);  
  }

  private void analyzeInterface(Token interfaceToken, UmpleInterface aInterface)
  {
    for(Token token : interfaceToken.getSubTokens())
    {
      if (token.is("depend"))
      {
        Depend d = new Depend(token.getValue());
        aInterface.addDepend(d);
      }
      if (token.is("interfaceMemberDeclaration"))
      {
        analyzeInterfaceMembers(token, aInterface);
      }
      else if (token.is("elementPosition"))
      {
        aInterface.setPosition(new Coordinate(token.getIntValue("x"),token.getIntValue("y"), token.getIntValue("width"), token.getIntValue("height")));
      }

    }
  }

  private void addUnlinkedInterfaceExtends()
  {  
    for (UmpleInterface child : unlinkedInterfaceExtends.keySet())
    {
      List<String> extendsNames = unlinkedInterfaceExtends.get(child);

      if (extendsNames == null)
      {
        continue;
      }

      for (int i=0; i < extendsNames.size();i++){
        String extendName= extendsNames.get(i);
          UmpleInterface uInterface=  model.getUmpleInterface(extendName);
          child.addExtendsInterface(uInterface);
      }
    }
  }  

  private void analyzeInterfaceMembers(Token interfaceMemberToken, UmpleInterface aInterface)
  {
    for(Token childToken : interfaceMemberToken.getSubTokens())
    {
      addExtendsTo(interfaceMemberToken, aInterface);
      if(childToken.is("abstractMethodDeclaration"))
      {
        analyzeMethod(childToken, aInterface);   
      }  
      else if (childToken.is("constantDeclaration"))
      {
        analyzeConstant(childToken, aInterface);    
      }
      else if (childToken.is("extraCode"))
      {
        aInterface.appendExtraCode(childToken.getValue("extraCode"));
      }
    }
  }

  private void analyzeAssociationClass(Token classToken)
  {
    AssociationClass aClass = model.addAssociationClass(classToken.getValue("name"));
    addExtendsTo(classToken, aClass);
    analyzeAllTokens(classToken,aClass,1);
    analyzeAllTokens(classToken,aClass,2);
    
    aClass.setPackageName(currentPackageName);

    Token leftAssociationToken = null;
    Token rightAssociationToken = null;

    for(Token token : classToken.getSubTokens())
    {
      if (token.is("singleAssociationEnd"))
      {
        if (leftAssociationToken == null)
        {
          leftAssociationToken = token;
        }
        else
        {
          rightAssociationToken = token;
        }
      }
    }

    if (leftAssociationToken == null || rightAssociationToken == null)
    {
      setFailedPosition(classToken.getPosition());
      return;
    }

    String innerName = null;
    String innerType = aClass.getName();
    String innerModifier = null;
    Multiplicity innerMult = new Multiplicity();
    innerMult.setRange("0","1"); 

    String leftName = leftAssociationToken.getValue("roleName");
    String leftType = leftAssociationToken.getValue("type");
    String leftModifier = leftAssociationToken.getValue("modifier");
    String leftBound = leftAssociationToken.getValue("bound");
    String leftLowerBound = leftAssociationToken.getValue("lowerBound");
    String leftUpperBound = leftAssociationToken.getValue("upperBound");
    Multiplicity leftMult = new Multiplicity();
    leftMult.setBound(leftBound);
    leftMult.setRange(leftLowerBound,leftUpperBound);

    String rightName = rightAssociationToken.getValue("roleName");
    String rightType = rightAssociationToken.getValue("type");
    String rightModifier = rightAssociationToken.getValue("modifier");
    String rightBound = rightAssociationToken.getValue("bound");
    String rightLowerBound = rightAssociationToken.getValue("lowerBound");
    String rightUpperBound = rightAssociationToken.getValue("upperBound");
    Multiplicity rightMult = new Multiplicity();
    rightMult.setBound(rightBound);
    rightMult.setRange(rightLowerBound,rightUpperBound);

    AssociationEnd leftFirstEnd = new AssociationEnd(innerName,innerType,innerModifier,leftType,leftMult);
    AssociationEnd leftSecondEnd = new AssociationEnd(leftName,leftType,leftModifier,innerType,innerMult);

    AssociationEnd rightFirstEnd = new AssociationEnd(innerName,innerType,innerModifier,rightType,rightMult);
    AssociationEnd rightSecondEnd = new AssociationEnd(rightName,rightType,rightModifier,innerType,innerMult);    

    updateAssociationEnds(leftFirstEnd,leftSecondEnd);
    updateAssociationEnds(rightFirstEnd,rightSecondEnd);

    Association leftAssociation = new Association(true,true,leftFirstEnd,leftSecondEnd);
    Association rightAssociation = new Association(true,true,rightFirstEnd,rightSecondEnd);

    model.addAssociation(leftAssociation);
    model.addAssociation(rightAssociation);

    unlinkedAssociations.add(leftAssociation);
    unlinkedAssociations.add(rightAssociation);
  }
  
  private boolean verifyClassesInUse()
  {
    for(Map.Entry<Position, String> e : positionToClassNameReference.entrySet())
    {
      if (model.getUmpleClass(e.getValue()) == null)
      {
        UmpleClass aClass = model.addUmpleClass(e.getValue());
        aClass.setPackageName(model.getDefaultNamespace());
        setFailedPosition(e.getKey());
        return false;
      }
    }
    return true;
  }
  
  private void addUnlinkedAssociationVariables()
  {
    for (AssociationVariable av : unlinkedAssociationVariables)
    {
      UmpleClass aClass = model.getUmpleClass(av.getType());
      UmpleClass bClass = model.getUmpleClass(av.getRelatedAssociation().getType()); 
      aClass.addAssociationVariable(av.getRelatedAssociation());
      aClass.addAssociation(bClass.getAssociation(bClass.indexOfAssociationVariable(av)));

      if (av.getIsNavigable())
      {
        bClass.addReferencedPackage(aClass.getPackageName());
      }

      if (av.getRelatedAssociation().getIsNavigable())
      {
        aClass.addReferencedPackage(bClass.getPackageName());
      }

    }
  }

  private boolean isUmpleClass(String elementName)
  {
    return (model.getUmpleInterface(elementName) != null) ? false: true;
  }

  private void addUnlinkedExtends()
  {  
    for (UmpleClass child : unlinkedExtends.keySet())
    {
      List<String> extendsNames = unlinkedExtends.get(child);

      if (extendsNames == null)
      {
        continue;
      }

      for (int i=0; i < extendsNames.size();i++){
        String extendName= extendsNames.get(i);
        if (isUmpleClass(extendName))
        {
          UmpleClass parent = model.getUmpleClass(extendName); 
          child.setExtendsClass(parent);
        }
        else {
          UmpleInterface uInterface=  model.getUmpleInterface(extendName);
          child.addParentInterface(uInterface);
          addImplementedMethodsFromInterface(uInterface, child);
        }
      }
    }
  }  


  private void addImplementedMethodsFromInterface(UmpleInterface parentInterface, UmpleClass uClass)
  {
    //GET AND SET METHODS CHECK?
    if (parentInterface.hasMethods())
    {
      for (Method aMethod : parentInterface.getMethods())
      {
        boolean shouldAddMethod = verifyIfMethodIsConstructorOrGetSet(uClass, aMethod);
        if (!(uClass.hasMethod(aMethod)) && shouldAddMethod)
        {
          aMethod.setIsImplemented(true);
          uClass.addMethod(aMethod);
        }
      }
    }
  }

  private boolean verifyIfMethodIsConstructorOrGetSet(UmpleClass uClass, Method aMethod)
  {
    String methodName = aMethod.getName();

    //1. Verify if method to be added is a setter or a getter
    String accessorName = methodName.substring(0,3);
    if ((accessorName.equals("get")) || (accessorName.equals("set"))){
      String possibleAttributeName =   methodName.substring(3,methodName.length()).toLowerCase();
      Attribute attr = uClass.getAttribute(possibleAttributeName);
      if (attr != null){
        return false;
      }
    }
    //2. Verify if method to be added is a constructor
    if (aMethod.getType().equals("public")){
      uClass.appendExtraCode(aMethod.toString());
      return false;
    }  
    //3. Verify if method from interface is already part of the Class extracode
    String match = "public " + aMethod.getType() + " " + aMethod.getName();    
    if (uClass.getExtraCode().contains(match)){
      return false;
    }
    return true;
  }

  private void addUnlinkedAssociations()
  {
    for (Association association : unlinkedAssociations)
    {
      AssociationEnd myEnd = association.getEnd(0);
      AssociationEnd yourEnd = association.getEnd(1);

      UmpleClass myClass = model.getUmpleClass(myEnd.getClassName());
      UmpleClass yourClass = model.getUmpleClass(yourEnd.getClassName());

      AssociationVariable myAs = new AssociationVariable(myEnd.getRoleName(),myEnd.getClassName(),myEnd.getModifier(),null,myEnd.getMultiplicity(),association.getIsLeftNavigable());
      AssociationVariable yourAs = new AssociationVariable(yourEnd.getRoleName(),yourEnd.getClassName(),yourEnd.getModifier(),null,yourEnd.getMultiplicity(),association.getIsRightNavigable());
      myAs.setRelatedAssociation(yourAs);
      myClass.addAssociationVariable(yourAs);
      myClass.addAssociation(association);

      yourClass.addAssociationVariable(myAs);
      yourClass.addAssociation(association);


      if (myAs.getIsNavigable())
      {
        myClass.addReferencedPackage(yourClass.getPackageName());
      }

      if (yourAs.getIsNavigable())
      {
        yourClass.addReferencedPackage(myClass.getPackageName());
      }      

    }
  }

  private void analyzeMethod(Token method, UmpleElement uElement)
  {
    String modifier = "";
    Method aMethod = new Method("","","",false);
    for(Token token : method.getSubTokens())
    {
      if (token.is("modifier"))
      {
        modifier += " " + (token.getValue());
        aMethod.setModifier(modifier);
      }
      else if (token.is("type"))
      {
        aMethod.setType(token.getValue());
      }
      else if (token.is("methodDeclarator"))
      {
        analyzeMethodDeclarator(token, aMethod);
      }
      else if (token.is("code"))
      {
        aMethod.setMethodBody(new MethodBody(token.getValue()));
      }
    }  

    // Add method to Class or Interface
    if (uElement instanceof UmpleClass)
    {
      UmpleClass uClass = (UmpleClass) uElement;
      boolean shouldAddMethod = verifyIfMethodIsConstructorOrGetSet(uClass, aMethod);
      if (!uClass.hasMethod(aMethod) && shouldAddMethod ){
        uClass.addMethod(aMethod); 
      }
    }
    else if (uElement instanceof UmpleInterface)
    {
      UmpleInterface uInterface = (UmpleInterface) uElement;
      if (!uInterface.hasMethod(aMethod)){
        uInterface.addMethod(aMethod); 
      }
    }  
  }

  private void analyzeMethodDeclarator(Token token, Method aMethod)
  {
    for(Token methodToken : token.getSubTokens())
    {
      if (methodToken.is("methodName")){
        aMethod.setName(methodToken.getValue());
      }
      if (methodToken.is("parameterList")){
        for(Token parameterToken : methodToken.getSubTokens())
        {
          boolean isList = false;
          if (parameterToken.is("parameter")){
            String paramType="";
            if (parameterToken.getSubToken("type") != null){
              paramType = parameterToken.getSubToken("type").getValue();
            }
            if (parameterToken.getSubToken("list") != null){
              isList = parameterToken.getSubToken("list").getValue() != null;        
            }
            String paramName = parameterToken.getSubToken("name").getValue();
            MethodParameter aParameter  = new MethodParameter(paramName,paramType,null,null, false);
            aParameter.setIsList(isList);
            aMethod.addMethodParameter(aParameter);
          }
        }
      }
    }
  }

  private void analyzeConstant(Token constantToken, UmpleElement uElement)
  {
    Constant aConstant = new Constant("","","","");
    String modifier = "";
    // Create the Constant Object
    for(Token token : constantToken.getSubTokens())
    {
      if (token.is("modifier"))
      {
        modifier += " " + (token.getSubToken(0).getName());
        aConstant.setModifier(modifier);
      }
      else if (token.is("name"))
      {
        aConstant.setName(token.getValue());
      }
      else  if (token.is("type"))
      {
        aConstant.setType(token.getValue());
      }
      else  if (token.is("value"))
      {
        aConstant.setValue(token.getValue());
      }
    }  
    // Add constant to Class or Interface
    if (uElement instanceof UmpleClass)
    {
      UmpleClass uClass = (UmpleClass) uElement;
      uClass.addConstant(aConstant);
    }
    else if (uElement instanceof UmpleInterface)
    {
      UmpleInterface uInterface = (UmpleInterface) uElement;
      uInterface.addConstant(aConstant);
    }  
  }


  private void analyzeInjectionCode(Token injectToken, UmpleClass aClass)
  {
    String type = injectToken.is("beforeCode") ? "before" : "after";
    aClass.addCodeInjection(new CodeInjection(type,injectToken.getValue("operationName"),injectToken.getValue("code")));
  }

  private void analyzeKey(Token keyToken, UmpleClass aClass)
  {
    if (aClass.getKey().isProvided())
    {
      setFailedPosition(keyToken.getPosition());
    }

    if (keyToken.is("defaultKey"))
    {
      aClass.getKey().setIsDefault(true);
      return;
    }

    for(Token token : keyToken.getSubTokens())
    {
      if (!token.is("keyId"))
      {
        continue;
      }
      aClass.getKey().addMember(token.getValue());
    }
  }

  private void analyzeSymmetricReflexiveAssociation(Token symmetricReflexiveAssociationToken, UmpleClass aClass)
  {
    String myName = symmetricReflexiveAssociationToken.getValue("roleName");
    String myType = aClass.getName();
    String myModifier = "symmetricreflexive";
    String myBound = symmetricReflexiveAssociationToken.getValue("bound");
    String myLowerBound = symmetricReflexiveAssociationToken.getValue("lowerBound");
    String myUpperBound = symmetricReflexiveAssociationToken.getValue("upperBound");
    Multiplicity myMult = new Multiplicity();
    myMult.setBound(myBound);
    myMult.setRange(myLowerBound,myUpperBound);

    AssociationVariable myAs = new AssociationVariable(myName,myType,myModifier,null,myMult,true);
    AssociationVariable yourAs = new AssociationVariable(myName,myType,myModifier,null,myMult,true);

    myAs.setRelatedAssociation(yourAs);
    aClass.addAssociationVariable(yourAs);
  }

  private Association createAssociation(String navigation, AssociationEnd firstEnd, AssociationEnd secondEnd)
  {
    boolean isNavigable = "--".equals(navigation);
    boolean isFirstNavigable = "<-".equals(navigation) || isNavigable;
    boolean isSecondNavigable = "->".equals(navigation) || isNavigable;
    return new Association(isFirstNavigable,isSecondNavigable,firstEnd,secondEnd);
  }

  private Association analyzeAssociation(Token associationToken, String defaultMyType)
  {
    Token myMultToken = associationToken.getSubToken(0);

    String navigation = associationToken.getValue("arrow");
    Token yourMultToken = associationToken.getSubToken(2);

    String myName = myMultToken.getValue("roleName");
    String myType = myMultToken.getValue("type") == null ? defaultMyType : myMultToken.getValue("type");
    String myModifier = myMultToken.getValue("modifier");
    String myBound = myMultToken.getValue("bound");
    String myLowerBound = myMultToken.getValue("lowerBound");
    String myUpperBound = myMultToken.getValue("upperBound");
    Multiplicity myMult = new Multiplicity(); 
    myMult.setBound(myBound);
    myMult.setRange(myLowerBound,myUpperBound);

    if (!myMult.isValid())
    {
      setFailedPosition(myMultToken.getPosition());
      return null;
    }

    String yourName = yourMultToken.getValue("roleName");
    String yourType = yourMultToken.getValue("type");
    String yourModifier = yourMultToken.getValue("modifier");
    String yourBound = yourMultToken.getValue("bound");
    String yourLowerBound = yourMultToken.getValue("lowerBound");
    String yourUpperBound = yourMultToken.getValue("upperBound");
    Multiplicity yourMult = new Multiplicity();
    yourMult.setBound(yourBound);
    yourMult.setRange(yourLowerBound,yourUpperBound);

    AssociationEnd firstEnd = new AssociationEnd(myName,myType,myModifier,yourType,myMult);
    AssociationEnd secondEnd = new AssociationEnd(yourName,yourType,yourModifier,myType,yourMult);
    updateAssociationEnds(firstEnd,secondEnd);

    Association association = createAssociation(navigation,firstEnd,secondEnd);

    if (!association.isValid())
    {
      Token atFaultToken = association.whoIsInvalid() == 0 ? myMultToken : yourMultToken;
      setFailedPosition(atFaultToken.getPosition());
      return null;
    }

    positionToClassNameReference.put(yourMultToken.getPosition("type"),yourType);
    model.addAssociation(association);
    return association;
  }

  private void updateAssociationEnds(AssociationEnd firstEnd, AssociationEnd secondEnd)
  {
    if (firstEnd.getRoleName().length() == 0)
    { 
      String rawName = StringFormatter.toCamelCase(firstEnd.getClassName());
      String name = firstEnd.getMultiplicity().isMany() ? model.getGlossary().getPlural(rawName) : rawName;
      firstEnd.setRoleName(name);
      firstEnd.setIsDefaultRoleName(true);
    }

    if (secondEnd.getRoleName().length() == 0)
    {
      String rawName = StringFormatter.toCamelCase(secondEnd.getClassName());
      String name = secondEnd.getMultiplicity().isMany() ? model.getGlossary().getPlural(rawName) : rawName;
      secondEnd.setRoleName(name);
      secondEnd.setIsDefaultRoleName(true);
    }
  }

  private void analyzeinlineAssociation(Token inlineAssociationToken, UmpleClass aClass)
  {
    Association association = analyzeAssociation(inlineAssociationToken,aClass.getName());

    if (!getParseResult().getWasSuccess())
    {
      return;
    }

    AssociationEnd myEnd = association.getEnd(0);
    AssociationEnd yourEnd = association.getEnd(1);

    AssociationVariable myAs = new AssociationVariable(myEnd.getRoleName(),myEnd.getClassName(),myEnd.getModifier(),null,myEnd.getMultiplicity(),association.getIsLeftNavigable());
    AssociationVariable yourAs = new AssociationVariable(yourEnd.getRoleName(),yourEnd.getClassName(),yourEnd.getModifier(),null,yourEnd.getMultiplicity(),association.getIsRightNavigable());
    myAs.setRelatedAssociation(yourAs);

    unlinkedAssociationVariables.add(yourAs);
    aClass.addAssociationVariable(yourAs);
    aClass.addAssociation(association);
  }

  private void analyzeAttribute(Token attributeToken, UmpleClass aClass)
  {
    boolean isAutounique = attributeToken.getValue("autounique") != null;
    boolean isUnique = attributeToken.getValue("unique") != null;
    boolean isLazy = attributeToken.getValue("lazy") != null;
    String modifier = attributeToken.getValue("modifier");
    String type = attributeToken.getValue("type");
    String name = attributeToken.getValue("name");
    String value = attributeToken.getValue("value");
    String derivedValue = attributeToken.getValue("derivedValue");

    if (derivedValue != null)
    {
      value = derivedValue;
    }

    if ("defaulted".equals(modifier) && value == null)
    {
      setFailedPosition(attributeToken.getPosition());
      return;
    }

    if (isUnique)
    {
      UniqueIdentifier uniqueIdentifier = new UniqueIdentifier(name,type,modifier,value);
      aClass.setUniqueIdentifier(uniqueIdentifier);
      return;
    }

    if (isAutounique)
    {
      type = "Integer";
    }

    if (type == null)
    {
      type = "String";
    }

    Attribute attribute = new Attribute(name,type,modifier,value,isAutounique);
    attribute.setIsLazy(isLazy);
    boolean isList = attributeToken.getValue("list") != null;

    if (name == null)
    {
      String rawName = StringFormatter.toCamelCase(type); 
      name = isList ? model.getGlossary().getPlural(rawName) : rawName;
    }

    if (derivedValue != null)
    {
      attribute.setIsDerived(true);
    }

    attribute.setIsList(isList);
    aClass.addAttribute(attribute);

  }
private void analyzeStateMachineToken(Token token)
  {
    if (token.is("stateMachineDefinition"))
    {
      analyzeStateMachineDefinition(token);
    }
  }  
  
  // Analyze state machine related tokens within the context of an Umple class
  private void analyzeStateMachineToken(Token token, UmpleClass aClass, int analysisStep)
  {
    if (analysisStep != 1)
    {
      return;
    }
    
    if (token.is("stateMachine"))
    {
      Token subToken = token.getSubToken(0);
      if (subToken.is("enum") || subToken.is("inlineStateMachine"))
      {
        analyzeStateMachine(subToken,aClass);
      }
      else if (subToken.is("referencedStateMachine"))
      {
        analyzedReferencedStateMachine(subToken,aClass);
      }
    }
  }

  
  private void postTokenStateMachineAnalysis()
  {
    addReferencedStateMachines();
  }
  
  private void addReferencedStateMachines()
  {
    for (UmpleClass clazz : umpleClassToStateMachineDefinition.keySet())
    {
      Pair nameToDefinition = umpleClassToStateMachineDefinition.get(clazz);
      Token stateMachineToken = stateMachineNameToToken.get(nameToDefinition.getValue());
      if (stateMachineToken == null)
      {
        continue;
      }

      StateMachine sm = new StateMachine(nameToDefinition.getName());
      sm.setUmpleClass(clazz);

      // analyze meta states

      populateStateMachine(stateMachineToken,sm);
    }
  }
  
  private void analyzeStateMachineDefinition(Token stateMachineDefinitionToken)
  {
    StateMachine smd = analyzeStateMachine(stateMachineDefinitionToken,null);
    model.addStateMachineDefinition(smd);
  }

  private void analyzedReferencedStateMachine(Token stateMachineToken, UmpleClass aClass)
  {
    String name = stateMachineToken.getValue("name");
    String value = stateMachineToken.getValue("definitionName");
    umpleClassToStateMachineDefinition.put(aClass,new Pair(name,value));


    // analyze meta states
  }

  private StateMachine analyzeStateMachine(Token stateMachineToken, UmpleClass aClass)
  {
    placeholderStateMachine = new StateMachine("PLACE_HOLDER");
    String name = stateMachineToken.getValue("name");
    stateMachineNameToToken.put(name,stateMachineToken);
    StateMachine sm = new StateMachine(name);
    sm.setUmpleClass(aClass);
    populateStateMachine(stateMachineToken, sm);

    while (placeholderStateMachine.numberOfStates() > 0)
    {
      placeholderStateMachine.getState(0).setStateMachine(sm);
    }
    return sm;
  }

  private State createStateFromTransition(Token transitionToken, StateMachine sm)
  {
    String name = transitionToken.getValue("stateName");
    State nextState = sm.findState(name);
    if (nextState == null)
    {
      nextState = placeholderStateMachine.findState(name);
    }

    if (nextState == null)
    {
      nextState = new State(transitionToken.getValue("stateName"),placeholderStateMachine);
    }
    return nextState;
  }

  private State createStateFromDefinition(Token stateToken, StateMachine sm)
  {
    State s = sm.findState(stateToken.getValue("stateName"),false);
    if (s == null)
    {
      s = placeholderStateMachine.findState(stateToken.getValue("stateName"));
      if (s != null)
      {
        s.setStateMachine(sm);
      }
    }
    if (s == null)
    {
      s = new State(stateToken.getValue("stateName"),sm);
    }
    return s;
  }

  private void populateStateMachine(Token stateMachineToken, StateMachine sm)
  {
    boolean isFirst = true;
    for(Token stateToken : stateMachineToken.getSubTokens())
    {
      if (!stateToken.is("state") && !stateToken.is("stateName"))
      {
        continue;
      }

      State s = createStateFromDefinition(stateToken,sm);
      if (isFirst)
      {
        s.setIsStartState(true);
      }
      isFirst = false;
      analyzeState(stateToken, s);
    }
  }

  private void analyzeState(Token stateToken, State fromState)
  {
      boolean addNewSm = true;
      boolean isConcurrentState = false;
    for(Token subToken : stateToken.getSubTokens())
    {
      if (subToken.is("transition"))
      {
        analyzeTransition(subToken, fromState); 
        continue;
      }
      else if (subToken.is("activity"))
      {
        analyzeActivity(subToken, fromState);
      }
      else if (subToken.is("entryOrExitAction"))
      {
        Action action = new Action(subToken.getValue("actionCode"));
        action.setActionType(subToken.getValue("type"));
        fromState.addAction(action);
      }
      else if (subToken.is("||"))
      {
        if (fromState.numberOfNestedStateMachines() == 0) { continue; }
        int previousSmIndex = fromState.numberOfNestedStateMachines() - 1;
              StateMachine nestedSm = fromState.getNestedStateMachine(previousSmIndex);
              if (nestedSm.numberOfStates() == 0) { continue; }
              nestedSm.setName(nestedSm.getState(0).getName());
              addNewSm = true;
              isConcurrentState = true;
      }
      else if (subToken.is("state"))
      {
        StateMachine nestedStateMachine = null;
        if (addNewSm)
        {
          nestedStateMachine = new StateMachine(fromState.getName());
          fromState.addNestedStateMachine(nestedStateMachine);
        }
        else
        {
          int lastIndex = fromState.numberOfNestedStateMachines() - 1;
          nestedStateMachine = fromState.getNestedStateMachine(lastIndex);
        }
        State s = createStateFromDefinition(subToken,nestedStateMachine);
        //alignStateDefinitionWithStateMachine(s,nestedStateMachine);
        if (addNewSm)
        {
                  if (isConcurrentState)
                  {
                    nestedStateMachine.setName(s.getName());
                  }
          s.setIsStartState(true);
        }
        addNewSm = false;
        analyzeState(subToken, s);
      }
    }
  }

  private void analyzeActivity(Token activityToken, State fromState)
  {
    new Activity(activityToken.getValue("activityCode"),fromState);
  }


  private void analyzeTransition(Token transitionToken, State fromState)
  {
    State nextState = createStateFromTransition(transitionToken,fromState.getStateMachine());
    Transition t = new Transition(fromState, nextState);

    String eventName = transitionToken.getValue("event");
    String eventTimerAmount = transitionToken.getValue("timer");

    if (eventName == null && eventTimerAmount != null)
    {
      eventName = fromState.newTimedEventName(nextState);
    }

    Token guardToken = transitionToken.getSubToken("guard");
    if (guardToken != null)
    {
      t.setGuard(new Guard(guardToken.getValue("guardCode")));
    }

    Token actionToken = transitionToken.getSubToken("action");
    if (actionToken != null)
    {
      t.setAction(new Action(actionToken.getValue("actionCode")));
    }

    if (eventName != null)
    {
      StateMachine sm = fromState.getStateMachine();
      UmpleClass uClass = sm.getUmpleClass();
      Event event = uClass == null ? sm.findOrCreateEvent(eventName) : uClass.findOrCreateEvent(eventName);
      if (eventTimerAmount != null)
      {
        event.setIsTimer(true);
        event.setTimerInSeconds(eventTimerAmount);
      }
      t.setEvent(event);
    }

  }
private void analyzeTraceToken(Token token)
  {
    if (token.is("traceType"))
    {
      model.setTraceType(token.getValue("traceType"));
    }
  }
  
  // Process trace related tokens within the context of a class
  private void analyzeTraceToken(Token token, UmpleClass aClass, int analysisStep)
  {
    
    // Only process trace tokens once all other entities have been analyzed
    if (analysisStep != 2)
    {
      return;
    }

    if (token.is("trace"))
    {
      TraceItem traceItem = new TraceItem();
      traceItem.setUmpleClass(aClass);
      traceItem.setAttribute(traceItem.getUmpleClass().getAttribute(token.getValue("trace_attribute")));
      traceItem.setWhereClause(token.getValue("trace_where"));
    }
  }
  
  // Perform post token analysis on trace related elements of the Umple language
  private void postTokenTraceAnalysis()
  {
  }
private void analyzeLayoutToken(Token token)
  {

  }

  // There are currently no core tokens of concern in the context of an UmpleClass
  // This method is available if needed
  private void analyzeLayoutToken(Token token, UmpleClass aClass, int analysisStep)
  {
    if (analysisStep != 2)
    {
      return;
    }
    
    if (token.is("elementPosition"))
    {
      aClass.setPosition(new Coordinate(token.getIntValue("x"),token.getIntValue("y"), token.getIntValue("width"), token.getIntValue("height")));
    }
    else if (token.is("associationPosition"))
    {
      String name = token.getValue("name");
      Association assoc = model.getAssociation(name);

      if (assoc != null)
      {
        assoc.setName(name);
        for(Token position : token.getSubTokens())
        {
          if (position.is("coordinate"))
          {
            assoc.addPosition(new Coordinate(position.getIntValue("x"),position.getIntValue("y"),0,0));
          }
        }
      }
    }
  }

  // Perform post token analysis on core elements of the Umple language
  private void postTokenLayoutAnalysis()
  {
    layoutNewElements();
  }  

  // Look for any new elements and give them positions if undefined
  private void layoutNewElements()
  {
    // layout classes
    for (int i=0; i<model.numberOfUmpleClasses(); i++)
    {
      UmpleClass c = model.getUmpleClass(i);

      if (c.getPosition().getStatus() == Coordinate.Status.Defaulted)
      {
        // Do nothing
      }
      else if (c.getPosition().getStatus() == Coordinate.Status.Undefined)
      {
        c.setPosition(model.getDefaultClassPosition(i+1));
        c.getPosition().setStatus(Coordinate.Status.Defaulted);
      }
    }

    // layout interfaces
    for (int i=0; i<model.numberOfUmpleInterfaces(); i++)
    {
      UmpleInterface c = model.getUmpleInterface(i);

      if (c.getPosition().getStatus() == Coordinate.Status.Defaulted)
      {
        // Do nothing
      }
      else if (c.getPosition().getStatus() == Coordinate.Status.Undefined)
      {
        c.setPosition(model.getDefaultClassPosition(i+1));
        c.getPosition().setStatus(Coordinate.Status.Defaulted);
      }
    }

    // Layout associations
    for (int i=0; i<model.numberOfAssociations(); i++)
    {
      Association a = model.getAssociation(i);
      int numberOfPositions = a.numberOfPositions();

      if (numberOfPositions < 2)
      {
        Coordinate[] defaults = model.getDefaultAssociationPosition(a);

        a.addPosition(defaults[0]);
        a.addPosition(defaults[1]);
        a.getPosition(0).setStatus(Coordinate.Status.Defaulted);
        a.getPosition(1).setStatus(Coordinate.Status.Defaulted);
      }
    }    
  }
}