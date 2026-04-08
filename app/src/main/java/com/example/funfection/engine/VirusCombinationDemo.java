package com.example.funfection.engine;

import com.example.funfection.model.Virus;

import java.util.ArrayList;
import java.util.List;

/**
 * Small console demo showing how viruses are produced and combined.
 */
public final class VirusCombinationDemo {

    private VirusCombinationDemo() {
    }

    public static void main(String[] args) {
        List<Virus> viruses = createDemoViruses();

        System.out.println("Produced viruses:");
        for (int index = 0; index < viruses.size(); index++) {
            Virus virus = viruses.get(index);
            System.out.println((index + 1) + ". " + describe(virus));
        }

        System.out.println();
        System.out.println("Pairwise combinations:");
        for (int leftIndex = 0; leftIndex < viruses.size(); leftIndex++) {
            for (int rightIndex = leftIndex + 1; rightIndex < viruses.size(); rightIndex++) {
                Virus left = viruses.get(leftIndex);
                Virus right = viruses.get(rightIndex);
                Virus offspring = InfectionEngine.combine(left, right);
                System.out.println(label(left) + " + " + label(right) + " -> " + describe(offspring));
            }
        }

        System.out.println();
        System.out.println("Self combinations:");
        for (Virus virus : viruses) {
            Virus offspring = InfectionEngine.combine(virus, virus);
            System.out.println(label(virus) + " + " + label(virus) + " -> " + describe(offspring));
        }
    }

    private static List<Virus> createDemoViruses() {
        List<Virus> viruses = new ArrayList<Virus>();
        viruses.add(VirusFactory.fromSeed("Ada", "Apple"));
        viruses.add(VirusFactory.fromSeed("Ben", "Banana"));
        viruses.add(VirusFactory.fromSeed("Cora", "Cherry"));
        viruses.add(VirusFactory.fromSeed("Drew", "Date"));
        viruses.add(VirusFactory.fromSeed("Elle", "Elderberry"));
        return viruses;
    }

    private static String label(Virus virus) {
        return virus.getName() + " [" + virus.getGenome() + "]";
    }

    private static String describe(Virus virus) {
        return virus.getName()
                + " | family=" + virus.getFamily()
                + " | carrier=" + virus.getCarrier()
                + " | stats=" + virus.getInfectivity().score()
                + "/" + virus.getResilience().score()
                + "/" + virus.getChaos().score()
                + " | mutation=" + virus.hasMutation()
                + " | genome=" + virus.getGenome();
    }
}