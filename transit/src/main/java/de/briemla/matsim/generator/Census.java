package de.briemla.matsim.generator;

public class Census {

	private final String name;
	private final int inhabitants;
	private final int workplaces;

	public Census(String name, int inhabitants, int workplaces) {
		this.name = name;
		this.inhabitants = inhabitants;
		this.workplaces = workplaces;
	}

	public String getName() {
		return name;
	}

	public int getInhabitants() {
		return inhabitants;
	}

	public int getWorkplaces() {
		return workplaces;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + inhabitants;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + workplaces;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Census other = (Census) obj;
		if (inhabitants != other.inhabitants) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (workplaces != other.workplaces) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Census [name=" + name + ", inhabitants=" + inhabitants + ", workplaces=" + workplaces + "]";
	}

}
