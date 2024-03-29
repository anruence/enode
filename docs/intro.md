## `enode`简介

-  `enode`中有很多概念，信息密度比较高，上手的难度相应的也比较大，为了让开发人员更好的用起来，把一些常见的概念和设计思路记录在此

## 核心思想

不管是`DDD`也好，`CQRS`架构也好，虽然都做到了让领域对象不仅有**状态**，而且有**行为**，但还不够彻底。因为对象的行为总是“**被调用
**”的。因为贫血模型的情况下，对象是提供了数据让别人去操作或者说被别人使用；而充血模型的情况下，对象则是提供了数据和行为，但还是让别人去操作或者说被别人使用。

> 真正的面向对象编程中的对象应该是一个”活“的具有主观能动性的存在于内存中的客观存在，它们不仅有状态而且还有自主行为。

1. 对象的状态可以表现出来被别人看到，但是必须是只读的，没有人可以直接去修改一个对象的状态，它的状态必须是由它自己的行为导致自己的状态的改变。
2. 对象的行为就是对象所具有的某种功能。对象的行为本质上应该是对某个消息的主动响应，这里强调的是主动，就是说对象的行为不可以被别人使用，而只能自己主动的去表现出该行为。

## 核心概念

`enode`在使用便利性了做了很多尝试和努力，而且针对消息队列和`EventStore`的实现对开发者都是开放的，同时和`Spring`高度集成，开箱即用。

### 聚合根
- 聚合根的元数据
  - 聚合根id
  - 聚合根类型
- 聚合根的初始化
  - 聚合根的定义

  聚合根需要定义一个无参构造函数，因为聚合根初始化时使用了默认无参构造器
  ```java
    aggregateRootType.getDeclaredConstructor().newInstance();
  ```

### 消息

#### 命令消息
- `Command`是命令，是想法，意图，描述你想做什么，要注意不是行为，因为还没发生

#### 事件消息
- `Event`是想法实施后，引起了聚合根状态变化而产生出了事件（带有业务语义）

命令和事件的区别，两者都是消息，为什么要分开表示呢？

命令可以被拒绝。事件已经发生。

这可能是最重要的原因。在事件驱动的体系结构中，毫无疑问，引发的事件代表了已发生的事情。
现在，因为命令是我们想要发生的事情，并且事件已经发生了，所以当我们命名这些事情时，我们应该使用不同的词，命令一般是名词，事件一般是过去分词
举个例子，拿订单系统来说，我们有个外部支付系统的依赖。
当用户在支付系统完成支付后，支付系统会向订单系统发送一个`Command`，`MarkOrderAsPayed`（标记订单已支付），订单在处理这个`Command`时，获取当前订单，调用订单的标记已支付（行为），产生了`OrderPayed`（订单已支付）事件。

我们可以看到，命令通常由系统外调用，事件是由处理程序和系统中的其他代码提供的。

这是他们分开表示的另一个原因。概念清晰度。
命令和事件都是消息。但它们实际上是独立的概念，应该明确地对概念进行建模。
这两者我理解都是符合人类思维的，首先是各类器官感知到【事件】（`Event`），随后大脑产生一个【意图】（`Command`），如何实现这个意图，思考的维度是过程式的，过程中每一步，又会产生一些【事件】，这个事件又会被其他器官感知到，大脑决策。如此循环往复。

### CommandBus
重新思考`CommandBus`这种模式，为了支持`CommandBus`同步返回结果，一直以来是使用`TCP Server`的方式返回，在分布式系统中这样会出现一种网状调用，复杂的调用关系很难跟踪，同时维护困难。为了架构的简洁，引入消息通道来支撑点对点的消息通信。
原有的`Tcp Server`更像一种观察者模式的实现，引入消息通道后，增加消息缓冲区，是一种标准的生产-消费模型，这也是和观察者最核心的区别

### EventStore
- 存储聚合根产出的事件，是一种Append的方式存储，事件产生后不会发生更新
- 每一次变更都会有一个版本号

#### `Event Sourcing`
- 根据事件恢复聚合根的操作就叫做`Event Sourcing`

### MessageHandler
- 命令处理
- 事件处理

### Saga
- 编排（`Choreography`）
  参与者（子事务）之间的调用、分配、决策和排序，通过交换事件进行进行。是一种去中心化的模式，参与者之间通过消息机制进行沟通，通过监听器的方式监听其他参与者发出的消息，从而执行后续的逻辑处理。
  `enode`中使用的就是这种模式，聚合根间通过领域事件的传递，达到最终一致

- 控制（`Orchestration`）
  提供一个控制类，方便参与者之间的协调工作。事务执行的命令从控制类发起，按照逻辑顺序请求`Saga`
  的参与者，从参与者那里接受到反馈以后，控制类在发起向其他参与者的调用。所有`Saga`的参与者都围绕这个控制类进行沟通和协调工作。

## 隐性约定解读
### 一个命令一次只能修改一个聚合根
首先做这个限制是从业务研发的角度来考虑的，这会让命令的职责更加具体，便于问题的拆解，职责的划分，如果一个命令要修改多个聚合根，应该通过`Saga`来完成

加上这个约定后带来的收益：
- 同一个聚合根的命令操作都会路由到同一个分区，聚合根就可以常驻内存（`In-Memory`），这样就不必每次重建聚合根，缓存利用率聚合是100%，是一种大限度利用内存的设计
- 命令路由到同一个分区，命令的操作顺序就可以保障（携带聚合根的版本），这就保障了聚合根在同一时刻只有一个在操作，直接避免你了并发问题，因为在设计上是无锁的
- 关于命令操作顺序的保障，为了提升吞吐，要求队列是无序消费，但队列无序了怎么保证操作是有序的呢，这点就有点类似`flink`中的`Water Marker`的设计了，聚合根的`mailbox`会记录每个消息的版本，如果高版本的数据先到，数据就会暂存，等到中间的版本处理完成才处理，通过`mailbox`中的顺序保证了操作的有序

### 聚合内强一致性
聚合设计之初，边界划分其实就很明确，聚合内的强一致也是业务上期望的，实现起来大部分情况下也是单机事务可以搞定

### 聚合间只能通过领域消息交互
这个限制是为了避免使用`Unit of Work`，完全利用`Saga`的编排模式来实现聚合间最终一致性，比较符合人类大脑接收信息的过程