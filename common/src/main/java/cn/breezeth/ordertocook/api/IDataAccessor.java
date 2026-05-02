package cn.breezeth.ordertocook.api;

public interface IDataAccessor {
    Object getData(Object stack);
    void setData(Object stack, Object nbt);
}
