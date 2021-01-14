# 源码案例

## 1、FactoryBean接口的使用

​		一般情况下，Spring通过反射机制利用bean的class属性指定实现类来实例化bean。在某些情况下，实例化bean过程比较复杂，如果按照传统的方式，则需要在<bean>标签中提供大量的配置信息，配置方式的灵活性是受限的。为此，Spring可以通过实现FactoryBean的接口来定制实例化bean的逻辑。

​		1、创建Car对象

```java
package com.mashibing.test;

public class Car {

    private String name;
    private String brand;
    private Integer speed;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    @Override
    public String toString() {
        return "Car{" +
                "name='" + name + '\'' +
                ", brand=" + brand +
                ", speed=" + speed +
                '}';
    }
}

```

​		2、创建CarFactoryBean

```java
package com.mashibing.test;

import org.springframework.beans.factory.FactoryBean;

public class CarFactoryBean implements FactoryBean<Car> {

    private String carInfo;

    public String getCarInfo() {
        return carInfo;
    }

    public void setCarInfo(String carInfo) {
        this.carInfo = carInfo;
    }

    @Override
    public Car getObject() throws Exception {

        Car car = new Car();
        String[] split = carInfo.split(",");
        car.setName(split[0]);
        car.setBrand(split[1]);
        car.setSpeed(Integer.valueOf(split[2]));
        return  car;
    }

    @Override
    public Class<?> getObjectType() {
        return Car.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
```

​		3、修改配置文件

```xml
    <bean id="car" class="com.mashibing.test.CarFactoryBean" >
        <property name="carInfo" value="大黄蜂,玛莎拉蒂,250"></property>
    </bean>
```

​		4、测试代码

```java
public class MyTest {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("test.xml");
        Car car=(Car)context.getBean("car");
        System.out.println(car);
    }
}
```

## 2、扩展initPropertySources方法

​		1、继承具体的类并扩展实现

```java
package com.mashibing.test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {
    public MyClassPathXmlApplicationContext(String... configLocations){
        super(configLocations);
    }

    @Override
    protected void initPropertySources() {
        getEnvironment().setRequiredProperties("OS");
    }
}
```

​		2、编写测试类

```java
public class MyTest {
    public static void main(String[] args) {
        ApplicationContext context = new MyClassPathXmlApplicationContext("test2.xml");
        User user=(User)context.getBean("testbean");
        System.out.println("username:"+user.getUserName()+"  "+"email:"+user.getEmail());
    }
}
```

## 3、扩展实现customizeBeanFactory方法

​		此方法是用来实现BeanFactory的属性设置，主要是设置两个属性：

​		allowBeanDefinitionOverriding：是否允许覆盖同名称的不同定义的对象

​		allowCircularReferences：是否允许bean之间的循环依赖

```java
public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {

    MyClassPathXmlApplicationContext(String... locations){
        super(locations);
    }

    @Override
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        super.setAllowBeanDefinitionOverriding(true);
        super.setAllowCircularReferences(true);
        super.customizeBeanFactory(beanFactory);
    }
}

```

## 4、自定义配置文件标签

​		1、User.java

```java
package com.mashibing.selftag;

public class User {
    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

​		2、UserBeanDefinitionParser.java

```java
package com.mashibing.selftag;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

public class UserBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @SuppressWarnings("rawtypes")
    protected Class getBeanClass(Element element) {
        return User.class;
    }

    protected void doParse(Element element, BeanDefinitionBuilder bean) {
        String userName = element.getAttribute("userName");
        String email = element.getAttribute("email");
        if (StringUtils.hasText(userName)) {
            bean.addPropertyValue("userName", userName);
        }
        if (StringUtils.hasText(email)){
            bean.addPropertyValue("email", email);
        }

    }
}
```

​		3、MyNamespaceHandler.java

```java
package com.mashibing.selftag;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class MyNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {

        registerBeanDefinitionParser("msb", new UserBeanDefinitionParser());
    }

}
```

​		4、在resource目录下创建META-INF目录下，并创建三个文件

Spring.handlers

```properties
http\://www.mashibing.com/schema/user=com.mashibing.selftag.MyNamespaceHandler
```

Spring.schemas

```properties
http\://www.mashibing.com/schema/user.xsd=META-INF/user.xsd
```

user.xsd

```xml
<?xml version="1.0" encoding="UTF-8"?>
<schema xmlns="http://www.w3.org/2001/XMLSchema"
        targetNamespace="http://www.mashibing.com/schema/user"
        xmlns:tns="http://www.mashibing.com/schema/user"
        elementFormDefault="qualified">
    <element name="msb">
        <complexType>
            <attribute name ="id" type = "string"/>
            <attribute name ="userName" type = "string"/>
            <attribute name ="email" type = "string"/>
        </complexType>
    </element>
</schema>
```

​		5、创建配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aaa="http://www.mashibing.com/schema/user"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
    http://www.mashibing.com/schema/user http://www.mashibing.com/schema/user.xsd">

    <aaa:msb id = "testbean" userName = "lee" email = "bbb"/>
</beans>
```

​		6、编写测试类

```java
public class MyTest {
    public static void main(String[] args) {
        ApplicationContext context = new MyClassPathXmlApplicationContext("test2.xml");
        User user=(User)context.getBean("testbean");
        System.out.println("username:"+user.getUserName()+"  "+"email:"+user.getEmail());
    }
}
```

## 5、ignoreDependencyInterface与ignoreDependencyType

​		在阅读源码的时候，很多同学发现有这样的两个方法：

```java
		/**
	 * 自动装配时忽略的类
	 * 
	 * Ignore the given dependency type for autowiring:
	 * for example, String. Default is none.
	 * @param type the dependency type to ignore
	 */
	void ignoreDependencyType(Class<?> type);

	/**
	 * 自动装配时忽略的接口
	 * 
	 * Ignore the given dependency interface for autowiring.
	 * <p>This will typically be used by application contexts to register
	 * dependencies that are resolved in other ways, like BeanFactory through
	 * BeanFactoryAware or ApplicationContext through ApplicationContextAware.
	 * <p>By default, only the BeanFactoryAware interface is ignored.
	 * For further types to ignore, invoke this method for each type.
	 * @param ifc the dependency interface to ignore
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	void ignoreDependencyInterface(Class<?> ifc);
```

​		这两个方法在实际使用的时候，应用的并不是很多或者几乎不用，有兴趣的同学可以去看https://www.jianshu.com/p/3c7e0608ff1f这个帖子，了解他们的详细使用方法和区别。

## 6、自定义属性编辑器

​		在日常的工作中，我们经常遇到一些特殊的案例需要自定义属性的解析器来完成对应的属性解析工作，大家需要理解它的本质来进行随意的扩展工作,但是此处的扩展没有大家想象的那么简单，详细的流程讲课的时候我大概讲一下，但是要复杂很多。主要有两种方式：

第一种方式：

Address.java

```java
package com.mashibing.propertyEditor;

class Address {
    private String district;
    private String city;
    private String province;

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String toString() {
        return this.province + "省" + this.city + "市" + this.district + "区";
    }
}
```

Customer.java

```java
package com.mashibing.propertyEditor;

public class Customer {
    private String name;
    private Address address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
```

AddressPropertyEditor.java

```java
package com.mashibing.propertyEditor;

import java.beans.PropertyEditorSupport;

public class AddressPropertyEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
        try {
            String[] adds = text.split("-");
            Address address = new Address();
            address.setProvince(adds[0]);
            address.setCity(adds[1]);
            address.setDistrict(adds[2]);
            this.setValue(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

MyPropertyEditorRegistrar.java

```java
package com.mashibing.propertyEditor;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

public class MyPropertyEditorRegistrar implements PropertyEditorRegistrar {
    @Override
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        registry.registerCustomEditor(Address.class,new AddressPropertyEditor());
    }
}

```

propertyEditor.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
    <bean id="customer" class="com.mashibing.propertyEditor.Customer">
        <property name="name" value="Jack" />
        <property name="address" value="浙江-杭州-西湖" />
    </bean>
    <!--第一种方式-->
    <bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
        <property name="propertyEditorRegistrars">
            <list>
                <bean class="com.mashibing.propertyEditor.MyPropertyEditorRegistrar"></bean>
            </list>
        </property>
    </bean>
    <!--第二种方式-->
    <bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
        <property name="customEditors">
            <map>
                <entry key="com.mashibing.propertyEditor.Address">
                    <value>com.mashibing.propertyEditor.AddressPropertyEditor</value>
                </entry>
            </map>
        </property>
    </bean>
</beans>
```

Test.java

```java
import com.mashibing.ignore.ListHolder;
import com.mashibing.propertyEditor.Customer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test3 {

    public static void main(String[] args) {
        ApplicationContext ac = new ClassPathXmlApplicationContext("propertyEditor.xml");
        Customer c = ac.getBean("customer", Customer.class);
        //输出
        System.out.println(c.getAddress());
    }
}
```

## 7、如何向beanFactoryPostProcessors中添加自定义的BeanFactoryPostProcessor

​		其实在之前的课程中，我做过演示，如何添加自定义的BeanFactoryPostProcessors，只需要在xml文件中声明成为一个bean即可，但是在此处我们如何进行扩展呢？

```java
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {


    public MyClassPathXmlApplicationContext(String... configLocations){
        super(configLocations);
    }

    @Override
    protected void initPropertySources() {
        System.out.println("扩展initPropertySource");
        getEnvironment().setRequiredProperties("username");
    }

    @Override
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        super.setAllowBeanDefinitionOverriding(false);
        super.setAllowCircularReferences(false);
        super.addBeanFactoryPostProcessor(new MyBeanFactoryPostProcessor());
        super.customizeBeanFactory(beanFactory);
    }

}
```



​		