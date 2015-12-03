package leo.agents

import leo.datastructures.Pretty
import leo.datastructures.blackboard.{Event, DataType, Blackboard, Result}

/**
 * Interface to any agent in the architecture.
 * Contains
 * <ul>
 *	<li> Activation Controls, to enable / disable the agent </li>
 *  <li> Offers an execution mechanism to the action of the agent </li>
 *  <li> Support the selection mechanism of tasks for the blackboard </li>
 * </ul>
 */
trait TAgent extends Dependency[TAgent] {
  val name : String

	/**
	* Method to pinpoint the task, that can be currently executed.
	* Most importantly an agent with >0 openTasks will prevent
	* a [[leo.datastructures.blackboard.DoneEvent]] from beeing sent.
	*/
  def openTasks : Int

  private var _isActive : Boolean = true

  /**
  * This flag shows, whether an agent should be considered for execution.
  * In this fashion, the agent can not prevent a [[leo.datastructures.blackboard.DoneEvent]] from being sent.
  */
  def isActive : Boolean = _isActive
	
	/**
	* Sets the active status.
	*/
  def setActive(a : Boolean) = _isActive = a

  /**
  * This method is called, whenever the program is forcefully stopped.
  * It has to be implemented to reset internal stati or the agent cannot simply be terminated.
  */
  def kill()

  /**
  * Registers this agent in the System for execution.
  */
  def register() = Blackboard().registerAgent(this)
  
  /**
  * Unregisteres this agent in the system.
  */
  def unregister() = Blackboard().unregisterAgent(this)

  /**
   * Declares the agents interest in specific data.
   *
   * @return None -> The Agent does not register for any data changes. <br />
   *         Some(Nil) -> The agent registers for all data changes. <br />
   *         Some(xs) -> The agent registers only for data changes for any type in xs.
   */
  def interest : Option[Seq[DataType]]

  /*
--------------------------------------------------------------------------------------------
                        COMBINATORICAL AUCTION
--------------------------------------------------------------------------------------------
   */


  /**
   * This method should be called, whenever a formula is added to the blackboard.
   *
   * The filter then checks the blackboard if it can generate tasks from it,
   * that will be stored in the Agent.
   *
   * @param event - Newly added or updated formula
   */
  def filter(event : Event) : Unit


  /**
   *
   * Returns a a list of Tasks, the Agent can afford with the given realtive budget..
   */
  def getTasks : Iterable[Task]

  /**
   * @return true if the agent has tasks, false otherwise
   */
  def hasTasks : Boolean

  /**
   * Each task can define a maximum amount of money, they
   * want to posses.
   *
   * A process has to be careful with this barrier, for he
   * may never be doing anything if he has to low money.
   *
   * @return maxMoney
   */
  def maxMoney : Double

  /**
   * As getTasks with an infinite budget.
   *
   * @return - All Tasks that the current agent wants to execute.
   */
  def getAllTasks : Iterable[Task]

  /**
   *
   * Given a set of (newly) executing tasks, remove all colliding tasks.
   *
   * @param nExec - The newly executing tasks
   */
  def removeColliding(nExec : Iterable[Task]) : Unit

  /**
   * Removes all Tasks
   */
  def clearTasks() : Unit

  /**
   * <p>
   * This method is called after a task is run and
   * all filter where applied sucessfully
   * </p>
   * <p>
   * The Method is standard implemented as the empty Instruction.
   * </p>
   *
   * @param t The comletely finished task
   */
  def taskFinished(t : Task) : Unit

  def taskChoosen(t : Task) : Unit
}



/**
  * Common trait for all Agent Task's. Each agent specifies the
  * work it can do.
  *
  * The specific fields and accessors for the real task will be in
  * the implementation.
  *
  * @author Max Wisniewski
  * @since 6/26/14
  */
abstract class Task extends Pretty  {

  /**
    * Prints a short name of the task
    * @return
    */
  def name : String

  /**
    * Computes the result, a delta on the blackboard state,
    * of the task.
    * @return
    */
  def run : Result

  /**
    *
    * Returns a set of all Formulas that are read for the task.
    *
    * @return Read set for the Task.
    */
  def readSet() : Map[DataType, Set[Any]]

  /**
    *
    * Returns a set of all Formulas, that will be written by the task.
    *
    * @return Write set for the task
    */
  def writeSet() : Map[DataType, Set[Any]]

  /**
    *
    * Set of [[DataType]] touched by this Task.
    *
    * @return all [[DataType]] contained in this task.
    */
  val lockedTypes : Set[DataType] = readSet().keySet.union(writeSet().keySet)

  /**
    * Checks for two tasks, if they are in conflict with each other.
    *
    * @param t2 - Second Task
    * @return true, iff they collide
    */
  def collide(t2 : Task) : Boolean = {
    val t1 = this
    if(t1 eq t2) return true
    val sharedTypes = t1.lockedTypes.intersect(t2.lockedTypes)  // Restrict to the datatypes both tasks use.

    !sharedTypes.exists{d =>        // There exist no datatype
      val r1 : Set[Any] = t1.readSet().getOrElse(d, Set.empty[Any])
      val w1 : Set[Any] = t1.writeSet().getOrElse(d, Set.empty[Any])
      val r2 : Set[Any] = t2.readSet().getOrElse(d, Set.empty[Any])
      val w2 : Set[Any] = t2.writeSet().getOrElse(d, Set.empty[Any])

      (r1 & w2).nonEmpty || (r2 & w1).nonEmpty || (w1 & w2).nonEmpty
      true
    }
  }

  /**
    *
    * Defines the realtive bid of an agent for a task.
    * The result has to in [0,1].
    *
    * @return - Possible profit, if the task is executed
    */
  def bid : Double
}

/**
  * Defines dependencies between data considered for execution.
  *
  * For any data contained in `before` should be exeuted before this object.
  * Any data in `after` should be executed after this object.
  *
  * @tparam A - arbitrary Types
  */
trait Dependency[A] {
  /**
    * A set of all data, that should be executed before this object.
    *
    * @return all data to be executed before
    */
  def before : Set[A]

  /**
    * A set of all data, that should be executed after this object.
    *
    * @return all data to be executed afterwards
    */
  def after : Set[A]
}