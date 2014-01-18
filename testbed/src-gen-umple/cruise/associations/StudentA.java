/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE ${last.version} modeling language!*/

package cruise.associations;

// line 10 "../../../src/TestHarnessAssociations.ump"
public class StudentA
{
  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  public @interface umplesourcefile{int[] line();String[] file();int[] javaline();int[] length();}

  //------------------------
  // MEMBER VARIABLES
  //------------------------

  //StudentA Associations
  private MentorA mentor;

  //------------------------
  // CONSTRUCTOR
  //------------------------

  public StudentA()
  {}

  //------------------------
  // INTERFACE
  //------------------------

  public MentorA getMentor()
  {
    return mentor;
  }

  public boolean setMentor(MentorA aNewMentor)
  {
    boolean wasSet = false;
    if (aNewMentor == null)
    {
      MentorA existingMentor = mentor;
      mentor = null;
      
      if (existingMentor != null && existingMentor.getStudent() != null)
      {
        existingMentor.setStudent(null);
      }
      wasSet = true;
      return wasSet;
    }

    MentorA currentMentor = getMentor();
    if (currentMentor != null && !currentMentor.equals(aNewMentor))
    {
      currentMentor.setStudent(null);
    }

    mentor = aNewMentor;
    StudentA existingStudent = aNewMentor.getStudent();

    if (!equals(existingStudent))
    {
      aNewMentor.setStudent(this);
    }
    wasSet = true;
    return wasSet;
  }

  public void delete()
  {
    if (mentor != null)
    {
      mentor.setStudent(null);
    }
  }

}