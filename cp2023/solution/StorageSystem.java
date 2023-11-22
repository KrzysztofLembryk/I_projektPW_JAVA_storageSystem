package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.exceptions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class StorageSystem implements cp2023.base.StorageSystem {
    private final Map<DeviceId, Integer> devTotalSlots;
    private final Map<ComponentId, DeviceId> compInDevPlacement;
    private final Map<ComponentId, Boolean> isCompBeingTransfered;
    private final Map<ComponentId, Semaphore> semaphoreComponentTransfer;
    private final Map<DeviceId, DeviceSpaceHandler> devSpacesHandlerMap;
    private final Map<DeviceId, Semaphore> semaphoresAccessDev;
    private final Semaphore semaphoreCheckTransfer;
    private final Map<DeviceId, SemaphoresDevSpacesHandler> semaphoresDevSpaces;
    private final Semaphore semaphoreGraph;
    private Graph transferGraph;

    private void initGraph() {
        Map<DeviceId, Integer> devFreeSpaces = new HashMap<>();
        for (DeviceId dev : devTotalSlots.keySet()) {
            devFreeSpaces.put(dev, devTotalSlots.get(dev));
        }

        for (ComponentId compId : compInDevPlacement.keySet()) {
            DeviceId currDev = compInDevPlacement.get(compId);
            Integer freeSpaces = devFreeSpaces.get(currDev);
            devFreeSpaces.put(currDev, freeSpaces - 1);
        }
        transferGraph = new Graph(devFreeSpaces, semaphoreComponentTransfer, devSpacesHandlerMap,
                semaphoresAccessDev);
    }

    public StorageSystem(Map<DeviceId, Integer> deviceTotalSlots,
                         Map<ComponentId, DeviceId> componentPlacement) {
        // Tworzymy nowa hashmape przechowujaca urzadzenia i ich pojemnosci
        // i robimy deep copy calej mapy deviceTotalSlots, gdyz systemy maja
        // dzialac niezaleznie. Wiec nie chcemy przechowywac oryginalu, gdyz
        // inny system jakos moglby go zmienic w trakcie dzialania tego sytemu.
        // Analogiczne deep copy robimy dla mapy componentPlacement.
        this.devTotalSlots = new HashMap<>();
        for(DeviceId dev : deviceTotalSlots.keySet())
        {
            Integer val = deviceTotalSlots.get(dev);
            this.devTotalSlots.put(new DeviceId(dev.hashCode()), val);
        }

        this.compInDevPlacement = new HashMap<>();
        for(ComponentId comp : componentPlacement.keySet())
        {
            DeviceId dev = new DeviceId(componentPlacement.get(comp).hashCode());
            compInDevPlacement.put(new ComponentId(comp.hashCode()), dev);
        }

        // Jednoczesnie z grafu moze korzystac jeden transfer i sprawdzac cykl
        // wiec potrzebujemy na to semafora.
        semaphoreGraph = new Semaphore(1, true);

        // Kazdy komponent ma swoj semafor na ktorym bedzie czekac jesli nie ma miejsca.
        semaphoreComponentTransfer = new ConcurrentHashMap<>();

        // Mapa sprawdzajaca czy dany komponent jest aktualnie transferowany.
        isCompBeingTransfered = new HashMap<>();
        for (ComponentId compId : componentPlacement.keySet())
            isCompBeingTransfered.put(compId, false);

        // SemaphoreCheckTransfer - chroni miejsce sprawdzania czy transfer jest poprawny.
        semaphoreCheckTransfer = new Semaphore(1, true);

        // semaphoresDev - mapa semaforow ktore pilnuja dostepu do devices, z permit=1.
        semaphoresAccessDev = new ConcurrentHashMap<>();
        for (DeviceId devId : deviceTotalSlots.keySet()) {
            semaphoresAccessDev.put(devId, new Semaphore(1, true));
        }

        // deviceSpacesMap - dla danego device trzyma devSpaceHandler w ktorym jest mapa
        // trzymajaca informacje ktore miejsca na urzadzeniu (0,...,capacity-1) sa zajete/wolne
        // i przez jakie komponenty sa one zajete.
        devSpacesHandlerMap = new ConcurrentHashMap<>();

        // Podobnie jak deviceSpaceMap trzyma analogiczna mape miejsc na danym urzadzeniu
        // tylko tym razem dla danego miejsca (0,...,capacity-1) trzyma semafor ktory wpuszcza
        // na dane miejsce, trzyma tez informacje o komponencie na danym miejscu
        semaphoresDevSpaces = new ConcurrentHashMap<>();

        try {
            for (DeviceId devId : this.devTotalSlots.keySet()) {
                Integer capacity = this.devTotalSlots.get(devId);
                semaphoresDevSpaces.put(devId, new SemaphoresDevSpacesHandler(capacity));
                devSpacesHandlerMap.put(devId, new DeviceSpaceHandler(capacity));
            }
            // Zajmujemy miejsca na urzadzeniach i semaforach.
            for (ComponentId compId : compInDevPlacement.keySet()) {
                DeviceId devId = compInDevPlacement.get(compId);

                // Dostajemy indeks zajetego miejsca na urzadzeniu.
                Integer idx =
                        devSpacesHandlerMap.get(devId).init_spaces_reservation(compId);

                // Zajmujemy semafor tego miejsca.
                semaphoresDevSpaces.get(devId).acquire(idx, compId);
            }
            initGraph();
        } catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }
    }

    private static TypeOfTransfer setTransferType(DeviceId srcDevId, DeviceId destDevId) {
        if (srcDevId == null && destDevId == null)
            return TypeOfTransfer.WRONG;
        else if (srcDevId == null && destDevId != null)
            return TypeOfTransfer.ADD;
        else if (srcDevId != null && destDevId == null)
            return TypeOfTransfer.REMOVE;
        else
            return TypeOfTransfer.TRANSFER;
    }

    private void isTransferOK(TypeOfTransfer transferType, ComponentId compId,
                              DeviceId srcDevId, DeviceId destDevId)
            throws TransferException
    {
        if (transferType == TypeOfTransfer.ADD) {
            // Nie ma device na ktore chcemy dodac komponent.
            if (!devTotalSlots.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);

            // Komponent juz istnieje w systemie.
            if (compInDevPlacement.containsKey(compId))
                throw new ComponentAlreadyExists(compId, destDevId);
        }
        else if (transferType == TypeOfTransfer.REMOVE) {
            // Nie ma device z ktorego chcemy usunac komponent.
            if (!devTotalSlots.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);

            // Nie ma komponentu ktory chcemy usunac lub nie ma go
            // na urzadznieu z ktorego chcemy go usunac.
            if (!compInDevPlacement.containsKey(compId) ||
                    !compInDevPlacement.get(compId).equals(srcDevId))
                throw new ComponentDoesNotExist(compId, srcDevId);
        }
        else if (transferType == TypeOfTransfer.TRANSFER) {
            // Brak srcDev lub destDev w mapie dostepnych urzadzen.
            if (!devTotalSlots.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if (!devTotalSlots.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);

            // Podany komponent nie istnieje lub istnieje
            // ale jest na innym srcDev.
            if (!compInDevPlacement.containsKey(compId) ||
                    !compInDevPlacement.get(compId).equals(srcDevId))
                throw new ComponentDoesNotExist(compId, srcDevId);

            // srcDev = destDev wiec nie trzeba transferowac komponentu.
            if (compInDevPlacement.get(compId).equals(destDevId))
                throw new ComponentDoesNotNeedTransfer(compId, destDevId);
        }
        else if (transferType == TypeOfTransfer.WRONG) {
            throw new IllegalTransferType(compId);
        }

    }

    private void checkIsCompBeingTransferred(ComponentId compId, TypeOfTransfer transferType)
            throws ComponentIsBeingOperatedOn {
        // Wystarczy sprawdzic tylko typ transferu bo to czy jest dobry sprawdzilismy
        // juz we wczesniej wywolanej funkcji isTransferOK.
        if (transferType == TypeOfTransfer.ADD) {
            isCompBeingTransfered.put(compId, false);
        }
        if (!isCompBeingTransfered.get(compId))
            isCompBeingTransfered.put(compId, true);
        else
            throw new ComponentIsBeingOperatedOn(compId);
    }

    private void do_the_TRANSFER(ComponentTransfer transfer, Integer idxOfMySpace)
            throws InterruptedException {
        DeviceId srcDevId, destDevId;
        srcDevId = transfer.getSourceDeviceId();
        destDevId = transfer.getDestinationDeviceId();
        ComponentId compId = transfer.getComponentId();

        // Jesli weszlismy do do_the_TRANSFER to znaczy ze otrzymalismy miejsce
        // na urzadzeniu i zwolnilismy swoje miejsce w srcDev,
        // wiec mozemy od razu zrobic transfer prepare.
        transfer.prepare();

        // Po naszym prepare inny transfer moze juz robic perform na nasze miejsce
        // wiec zwalniamy semafor blokujacy nasze miejsce na srcDev.
        semaphoresDevSpaces.get(srcDevId).release(compId);

        // Czekamy az nam zostanie zwolniony semafor miejsca na naszym destDev,
        // zebysmy mogli zrobic swoje perform.
        semaphoresDevSpaces.get(destDevId).acquire(idxOfMySpace, compId);

        transfer.perform();

        // Usuwamy semafor dla naszego komponentu, nie musi to byc s.kryt bo
        // komponenty sa unikalne i uzywamy concurrent hashmap.
        semaphoreComponentTransfer.remove(compId);

        // Ustawiamy ze nasz komponent nie jest juz transferowany i zmieniamy
        // w mapie komponentow przypisana do niego wartosc device.
        semaphoreCheckTransfer.acquire();

        isCompBeingTransfered.put(compId, false);
        compInDevPlacement.put(compId, destDevId);

        semaphoreCheckTransfer.release();
    }

    private void do_REMOVING(ComponentTransfer transfer)
            throws InterruptedException {
        DeviceId srcDevId;
        ComponentId compId = transfer.getComponentId();
        srcDevId = transfer.getSourceDeviceId();

        // Analogicznie jak w metodzie do_the_TRANSFER, tylko nie musimy
        // czekac na semafor destDev bo jedynie usuwamy komponent z srcDev.

        transfer.prepare();

        semaphoresDevSpaces.get(srcDevId).release(compId);

        transfer.perform();

        semaphoreComponentTransfer.remove(compId);

        semaphoreCheckTransfer.acquire();

        isCompBeingTransfered.remove(compId);
        compInDevPlacement.remove(compId);

        semaphoreCheckTransfer.release();
    }

    private void do_ADDING(ComponentTransfer transfer, Integer idxOfMySpace)
            throws InterruptedException {
        DeviceId destDevId = transfer.getDestinationDeviceId();
        ComponentId compId = transfer.getComponentId();

        // Analogicznie jak w do_the_TRANSFER tylko nie zwalniamy semafora
        // z srcDev, gdyz dodajemy nowy element do systemu i srcDev = null.

        transfer.prepare();

        semaphoresDevSpaces.get(destDevId).acquire(idxOfMySpace, compId);

        transfer.perform();

        semaphoreComponentTransfer.remove(compId);

        semaphoreCheckTransfer.acquire();

        isCompBeingTransfered.put(compId, false);
        compInDevPlacement.put(compId, destDevId);

        semaphoreCheckTransfer.release();
    }


    @Override
    public void execute(ComponentTransfer transfer) throws TransferException {
        DeviceId srcDevId, destDevId;
        ComponentId compId = transfer.getComponentId();
        srcDevId = transfer.getSourceDeviceId();
        destDevId = transfer.getDestinationDeviceId();
        TypeOfTransfer transferType = setTransferType(srcDevId, destDevId);

        try {
            // Sprawdzamy czy transfer jest poprawny.
            semaphoreCheckTransfer.acquire();
            isTransferOK(transferType, compId, srcDevId, destDevId);
            checkIsCompBeingTransferred(compId, transferType);
            semaphoreCheckTransfer.release();

            // Dodajemy semafor dla komponentu ktory bedzie transferowany.
            semaphoreComponentTransfer.put(compId, new Semaphore(0, true));

            // W zaleznosci od typu transferu robimy rozne rzeczy.
            switch (transferType) {
                case ADD -> {

                    // Zawsze najpierw sprawdzamy czy jest cykl, czy jest wolne miejsce
                    // badz czy musimy czekac. W zaleznosci od tego dodajemy swoj transfer
                    // do grafu transferow badz nie.
                    semaphoreGraph.acquire();

                    transferGraph.checkCycle(srcDevId, destDevId, compId);

                    semaphoreGraph.release();

                    // Jesli nie bylo miejsca to zawieszamy sie na semaforze naszego komponentu.
                    semaphoreComponentTransfer.get(compId).acquire();

                    // Jesli doszlismy tutaj to znaczy ze albo bylo miejsce i zaraz zostanie nam
                    // ono przydzielone, albo byl cykl i mamy juz przydzielone miejsce tylko musimy
                    // dostac jego indeks. Na danym urzadzeniu tylko jeden transfer w danej chwili
                    // moze szukac miejsca.
                    semaphoresAccessDev.get(destDevId).acquire();

                    Integer idxOfMySpace =
                            devSpacesHandlerMap.get(destDevId).reserveSpace(compId);

                    semaphoresAccessDev.get(destDevId).release();

                    do_ADDING(transfer, idxOfMySpace);
                }
                case REMOVE -> {

                    semaphoreGraph.acquire();

                    transferGraph.freeSpaceOnDev(srcDevId, compId);

                    semaphoreGraph.release();

                    do_REMOVING(transfer);
                }
                case TRANSFER -> {

                    semaphoreGraph.acquire();

                    transferGraph.checkCycle(srcDevId, destDevId, compId);

                    semaphoreGraph.release();

                    semaphoreComponentTransfer.get(compId).acquire();

                    semaphoresAccessDev.get(destDevId).acquire();
                    Integer idxOfMySpace =
                            devSpacesHandlerMap.get(destDevId).reserveSpace(compId);
                    semaphoresAccessDev.get(destDevId).release();

                    do_the_TRANSFER(transfer, idxOfMySpace);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }
    }
}
