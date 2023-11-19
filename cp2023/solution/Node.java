package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

import java.util.Vector;
import java.util.concurrent.Semaphore;

public class Node {
    private final DeviceId devId;
    // wektor czekajacych na miejsce na devId transferow
    // gdzie transfer na msc 0 ma najwiekszy priorytet
    private Vector<Node> vecTransfersToMe;
    // nie mozemy miec wektora semaforow tutaj, ale bedziemy miec wektor
    // komponentow gdzie na i=0 mamy transfer dla danego komponentu o
    // najwiekszym priorytecie, na zewnatrz Node'a bedziemy miec mape semaforow
    // dla danego komponentu z liczba wejsc 0.
    private Vector<ComponentId> vecComponentsPriorities;
    public Node(DeviceId id)
    {
        devId = id;
        vecTransfersToMe = new Vector<>();
        vecComponentsPriorities = new Vector<>();
    }

    protected Integer addEdge(Node n, ComponentId comp)
    {
        vecTransfersToMe.add(n);
        vecComponentsPriorities.add(comp);
        // po dodaniu krawedzi zwracamy indeks gdzie ta krawedz jest
        // czyli jaki ma priorytet
        return vecTransfersToMe.size() - 1;
    }
    protected  ComponentId removeEdge(int idx) throws Exception
    {
        if(idx < vecTransfersToMe.size() && idx >= 0)
        {
            vecTransfersToMe.remove(idx);
            ComponentId compId = vecComponentsPriorities.get(idx);
            vecComponentsPriorities.remove(idx);
            return compId;
        }
        throw new Exception("Node - removeEdge - idx out of bounds");
    }
    protected DeviceId getDevId()
    {
        return devId;
    }
    protected Vector<Node> getTransfersToMe()
    {
        return vecTransfersToMe;
    }
}
