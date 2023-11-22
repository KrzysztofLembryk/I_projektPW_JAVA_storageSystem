package cp2023.solution;

import cp2023.base.ComponentId;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/** Klasa DeviceSpaceHandler zarzadza przydzielaniem miejsc (indeksow miejsc)
 * na urzadzeniach komponentom. Jesli istnieje wolne miejsce to przydzielane
 * jest pierwsze znalezione wolne miejsce. Jesli komponent ma zajac miejsce
 * innego komponentu to miejsce nie jest zwalniane tylko nowy komponent
 * jest wpisywany na miejsce starego
*/
public class DeviceSpaceHandler {
    private final Integer size;

    // Tworzymy specjalny komponent oznaczajacy wolne miejsce. Zakladam,
    // ze nie bedzie komponentu o indeksie Integer.Min_Val.
    private final ComponentId freeSpace = new ComponentId(Integer.MIN_VALUE);

    // W MapOfDevSpaces na poczatku inicjalizujemy przez wlozenie size
    // elementow typu freeSpace (indeksy miejsc sa 0...size-1).
    private Map<Integer, ComponentId> mapOfDevSpaces;
    public DeviceSpaceHandler(Integer size)
    {
        this.size = size;
        mapOfDevSpaces = new ConcurrentHashMap<>();

        for(int i = 0; i < size; i++)
        {
            mapOfDevSpaces.put(i,  freeSpace);
        }

    }
    private int initIdx = 0;
    protected Integer init_spaces_reservation(ComponentId compId)
    {
        if(initIdx < size)
        {
            if(mapOfDevSpaces.get(initIdx).equals(freeSpace))
            {
                mapOfDevSpaces.put(initIdx,  compId);
                initIdx += 1;
                return initIdx - 1;
            }
        }
        return -1;
    }

    public Integer reserveSpace(ComponentId compId)
            throws InterruptedException
    {
        // najpierw sprawdzamy czy juz nie mamy miejsca w destdev
        // moglo sie tak zdarzyc gdy byl cykl
        for(int i = 0; i < size; i++) {
            if (mapOfDevSpaces.get(i).equals(compId)) {
                return i;
            }
        }
        for(int i = 0; i < size; i++) {
            if (mapOfDevSpaces.get(i).equals(freeSpace)) {
                mapOfDevSpaces.put(i, compId);
                return i;
            }
        }
        // Tutaj nigdy nie wejdziemy, ale java o tym nie wie wiec musimy
        // cos zwracac.
        return -1;
    }
    public Integer reserveSpaceCycle(ComponentId newCompId, ComponentId oldCompId)
    {
        for(int i = 0; i < size; i++) {
            if(mapOfDevSpaces.get(i).equals(oldCompId)){
                mapOfDevSpaces.put(i, newCompId);
                return i;
            }
        }
        return -1;
    }
    public void freeSpace(ComponentId compId)
    {
        // we free occupied space by us
        for(int i = 0; i < size; i++)
        {
            if(mapOfDevSpaces.get(i).equals(compId))
            {
                mapOfDevSpaces.put(i, freeSpace);
                break;
            }
        }
    }
}
