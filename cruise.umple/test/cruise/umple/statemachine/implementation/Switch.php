<?php
/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.12.0.176 modeling language!*/

class Switch
{

  //------------------------
  // MEMBER VARIABLES
  //------------------------

  //Switch State Machines
  private $status;

  // status constants
  private static $StatusOn = 1;
  private static $StatusOff = 2;

  //------------------------
  // CONSTRUCTOR
  //------------------------

  public function __construct()
  {
    $this->setStatus(self::$StatusOn);
  }

  //------------------------
  // INTERFACE
  //------------------------

  public function getStatus()
  {
    if ($this->status == self::$StatusOn) { return "StatusOn"; }
    elseif ($this->status == self::$StatusOff) { return "StatusOff"; }
    return null;
  }

  public function press()
  {
    $wasEventProcessed = false;

    if ($this->status == self::$StatusOn)
    {
      $this->setStatus(self::$StatusOff);
      $wasEventProcessed = true;
    }

    return $wasEventProcessed;
  }

  private function setStatus($aStatus)
  {
    $this->status = $aStatus;

    // entry actions and do activities
    if ($this->status == self::$StatusOff)
    {
      keepDoing();
    }
  }

  public function equals($compareTo)
  {
    return $this == $compareTo;
  }

  public function delete()
  {}

}
?>