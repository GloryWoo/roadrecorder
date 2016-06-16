package harman.com.util.proxy;

import java.util.ArrayList;
import java.util.Collection;

/**
 * ArrayList  �Ĵ����� ��Ҫ��ʵ����ʽ���Ԫ��
 * example: ArrayListProxy arrayListProxy = new ArrayListProxy();
 * arrayListProxy.addObject(a).addObject(b).addObject(c);
 */
public class ArrayListProxy<E> extends ArrayList<E>{

   //��дArrayList�����й��캯��---start
    public ArrayListProxy(Collection<? extends E> c) {
        super(c);
    }


    public ArrayListProxy(int initialCapacity) {
        super(initialCapacity);
    }


    public ArrayListProxy() {
        super();
    }
   //��дArrayList�����й��캯��---end

    /**
     * �� ArrayList �� add() �ķ������з�ת����  ArrayListProxy ��ʵ�� ��ʽ���
     * @param e
     * @return 
     */
    public ArrayListProxy addObject(E e){
        if(this.add(e)){
            return this;
        }
      throw new ArrayStoreException("ArrayListProxy add element fail!");
    }

}