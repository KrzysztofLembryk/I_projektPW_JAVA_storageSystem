package cp2023.solution;

import cp2023.base.ComponentId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Klasa SemaphoresDevSpacesHandler jest odpowiednikiem klasy DevSpaceHandler
 * rezerwujacej miejsca, tylko ze tutaj robimy albo acquire na semaforze
 * dla danego miejsca, albo release na tym semaforze.
 *
 * Potrzebujemy semaforow na kazde miejsce na danym urzadzeniu, gdyz dzieki
 * temu wiemy kiedy transfer zrobil swoje prepare(). Bo po zrobieniu
 * prepare() storage system robi release na semaforze zajmowanym przez
 * transferowany komponent i dzieki temu inny transfer robi acquire tego miejsca
 * i moze zaczac wykonywac swoje perform.
 */
public class SemaphoresDevSpacesHandler{
    private final Integer size;
    private final ComponentId freeSpace = new ComponentId(Integer.MIN_VALUE);
    private Map<Integer, Pair<Semaphore, ComponentId>> semSpacesMap;

    public SemaphoresDevSpacesHandler(Integer size) throws InterruptedException
    {
        this.size = size;
        semSpacesMap = new ConcurrentHashMap<>();

        for(int i = 0; i < size; i++)
            semSpacesMap.put(i, new Pair<>(new Semaphore(1, true), freeSpace));

    }

    public void acquire(Integer idx, ComponentId compId) throws InterruptedException
    {
        semSpacesMap.get(idx).first.acquire();
        semSpacesMap.get(idx).second = compId;
    }
    public void release(ComponentId compId)
    {
        for(int i = 0; i < size; i++)
        {
            if(semSpacesMap.get(i).second.equals(compId))
            {
                semSpacesMap.get(i).second = freeSpace;
                semSpacesMap.get(i).first.release();
                break;
            }
        }
    }
}
