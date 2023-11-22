package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

import java.util.Vector;
import java.util.concurrent.Semaphore;

/**
 * Klasa Node przechowuje wierzcholek grafu, w ktorym szukamy
 * czy istnieja zacyklone transfery.
 *
 * Dany Node reprezentuje urzadzenie w naszym systemie i przechowuje
 * wektor Nodeow z ktorych ida transfery do tego Node'a (czyli urzadzenia).
 *
 * Klasa Node przechowuje rowniez wektor komponentow, ktore chca byc na
 * Node transferowane.
 *
 * Mamy odpowiedniosc: element na miejscu i-tym w wektorze komponentow, jest
 * transferowany z Node'a i-tego w wektorze Node'ow.
 *
 * Czyli w danym Node trzymamy krawedzie skierowane przychodzace do Node'a.
 *
 * Najwiekszy priorytet ma krawedz (transfer) na miejscu 0 w wektorach.
 *
 */
public class Node {
    private final DeviceId devId;
    private final Vector<Node> vecTransfersToMe;
    private final Vector<ComponentId> vecComponentsPriorities;
    public Node(DeviceId id)
    {
        devId = id;
        vecTransfersToMe = new Vector<>();
        vecComponentsPriorities = new Vector<>();
    }

    /**
     * Zeby dodac krawedz (transfer) idaca do naszego Node'a (urzadzenia),
     * potrzebujemy znac Node source'owy i jaki komponent tranfserujemy
     * z tego source'owego Node'a. Dodajemy te elementy na koniec wektora,
     * gdyz przyszly one pozniej niz wczesniej dodane elementy, wiec maja
     * mniejszy priorytet.
     */
    protected Integer addEdge(Node n, ComponentId comp)
    {
        vecTransfersToMe.add(n);
        vecComponentsPriorities.add(comp);

        return vecTransfersToMe.size() - 1;
    }

    /**
     * Usuwamy krawedz wchodzaca do Node'a o podanym priorytecie , gdyz moglismy
     * znalezc cykl ktory zawiera transfery o nie najwiekszych priorytetach.
     *
     * Po usunieciu krawedzi, zwracamy z jakiego srcDev ona szla i jaki komponent
     * chciala transferowac.
     */
    protected  Pair<ComponentId, DeviceId> removeEdge(int priority)
    {
        if(priority < vecTransfersToMe.size() && priority >= 0)
        {
            DeviceId srcDev = vecTransfersToMe.get(priority).getDevId();
            vecTransfersToMe.remove(priority);
            ComponentId compId = vecComponentsPriorities.get(priority);
            vecComponentsPriorities.remove(priority);

            return new Pair<>(compId, srcDev);
        }
        return new Pair<>(null, null);
    }
    protected  boolean noTransfersToMe()
    {
        return vecTransfersToMe.size() == 0;
    }
    protected DeviceId getDevId()
    {
        return devId;
    }
    protected Vector<Node> getTransfersToMe()
    {
        return vecTransfersToMe;
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof Node)) {
            return false;
        }
        return this.devId.equals(((Node)obj).devId);
    }
}
