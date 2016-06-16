package harman.com.util.proxy;


import java.util.HashMap;
import java.util.Map;


/** 
 *HashMap �Ĵ����� ʵ����ʽ���Ԫ�� 
 * example��  HashMapProxy hashMapProxy   = new HashMapProxy(); 
 *            hashMapProxy.putObject("a","b").putObject("c","d"); 
 * @param <K> 
 * @param <V> 
 */ 
public class HashMapProxy<K,V> extends HashMap<K,V> { 

   //��дHashMap�����й��캯��---start 
    public HashMapProxy(int initialCapacity) { 
        super(initialCapacity); 
    }

    public HashMapProxy() {
        super();
    }

    public HashMapProxy(Map<? extends K, ? extends V> m) {
        super(m);
    }


    public HashMapProxy(int initialCapacity, float loadFactor) {
          super(initialCapacity,loadFactor);
    }
   //��дHashMap�����й��캯��---end

    /**
     * �� HashMap �� put() �ķ������з�ת����  HashMapProxy ��ʵ�� ��ʽ���
     * @param key
     * @param value
     * @return 
     */
    public HashMapProxy putObject(K key,V value){
        this.put(key, value);
        return this;
    }

}