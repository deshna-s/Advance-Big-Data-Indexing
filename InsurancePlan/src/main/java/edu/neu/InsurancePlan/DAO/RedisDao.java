package edu.neu.InsurancePlan.DAO;


import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface RedisDao {
    public void postValue (String key, String value);
    public boolean deleteValue(String key);
    public Object getValue(String key);

    void hSet(String key, String field, String value);

    void hDelete(String key, String field);

    void setAdd(String key, String value);

    boolean hasKey(String key);

    Set<String> keys(String keyPattern);

    void deleteKey(String key);

    void deleteKeys(Collection<String> keys);

    Map<Object, Object> hGetAll(String key);

    void hDelete(String key);

    Set<String> sMembers(String key);

    String hGet(String key, String field);


}