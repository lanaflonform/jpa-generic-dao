/* Copyright 2013 David Wolverton
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package junit.googlecode.genericdao.dao.hibernate;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ObjectNotFoundException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import test.googlecode.genericdao.dao.hibernate.dao.PersonDAO;
import test.googlecode.genericdao.dao.hibernate.dao.PersonService;
import test.googlecode.genericdao.BaseTest;
import test.googlecode.genericdao.model.Person;

import com.googlecode.genericdao.dao.DAODispatcherException;
import com.googlecode.genericdao.dao.hibernate.DAODispatcher;
import com.googlecode.genericdao.dao.hibernate.GeneralDAO;
import com.googlecode.genericdao.search.ExampleOptions;
import com.googlecode.genericdao.search.Search;

@Transactional
public class GeneralDAOAndDispatcherTest extends BaseTest {
	private GeneralDAO generalDAO;
	private DAODispatcher dispatcher;
	private PersonDAO personDAO;
	private PersonService personService;

	@Autowired
	public void setGeneralDAO(GeneralDAO generalDAO) {
		this.generalDAO = generalDAO;
	}

	@Autowired
	public void setDAODispatcher(DAODispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Autowired
	public void setPersonDAO(PersonDAO personDAO) {
		this.personDAO = personDAO;
	}

	@Autowired
	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	@Test
	public void testGeneralDAO() {
		testDAO(generalDAO);
	}

	@Test
	public void testDispatcherWithGeneralDAO() {
		dispatcher.setSpecificDAOs(new HashMap<String, Object>());
		testDAO(dispatcher);
	}

	@Test
	public void testDispatcherWithSpecificDAO() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(Person.class.getName(), personDAO);
		dispatcher.setSpecificDAOs(map);

		testDAO(dispatcher);
	}

	@Test
	public void testDispatcherWithSpecificDAONoInterface() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(Person.class.getName(), personService);
		dispatcher.setSpecificDAOs(map);

		testDAO(dispatcher);
	}

	/**
	 * Just quickly check that all the methods basically work. The underlying
	 * implementation is more thoroughly tested in the
	 * <code>junit.googlecode.genericdao.dao.hibernate</code> package
	 */
	@SuppressWarnings("unchecked")
	private void testDAO(GeneralDAO dao) {
		Person fred = setup(new Person("Fred", "Smith", 35));
		Person bob = setup(new Person("Bob", "Jones", 58));
		Person cyndi = setup(new Person("Cyndi", "Loo", 58));
		Person marty = setup(new Person("Marty", "McFly", 58));

		
		assertTrue(dao.save(fred));
		assertTrue(dao.save(bob));
		fred.setFather(bob);

		assertEquals(bob, dao.find(Person.class, bob.getId()));
		assertEquals(fred, dao.find(Person.class, fred.getId()));

		assertListEqual(new Person[] { bob, fred }, dao
				.findAll(Person.class));
		assertListEqual(new Person[] { bob, fred }, dao
				.search(new Search(Person.class)));

		//count
		assertEquals(2, dao.count(new Search(Person.class)));
		
		//searchAndCount
		assertListEqual(new Person[] { bob, fred }, dao
				.searchAndCount(new Search(Person.class)).getResult());

		//searchUnique
		Search s = new Search(Person.class);
		s.addFilterEqual("id", bob.getId());
		assertEquals(bob, dao.searchUnique(s));
		
		//search
		s = new Search(Person.class);
		s.addFilterEqual("father.id", bob.getId());
		s.setResultMode(Search.RESULT_SINGLE);
		s.addField("firstName");
		assertEquals(fred.getFirstName(), dao.search(s).get(0));

		//searchUnique
		assertEquals(fred.getFirstName(), dao.searchUnique(s));
		
		//example
		Person example = new Person();
		example.setFirstName("Bob");
		example.setLastName("Jones");
		
		s = new Search(Person.class);
		s.addFilter(dao.getFilterFromExample(example));
		assertEquals(bob, dao.searchUnique(s));
		
		example.setAge(0);
		s.clear();
		s.addFilter(dao.getFilterFromExample(example));
		assertEquals(null, dao.searchUnique(s));
		
		s.clear();
		s.addFilter(dao.getFilterFromExample(example, new ExampleOptions().setExcludeZeros(true)));
		assertEquals(bob, dao.searchUnique(s));
		
		//test nulls
		try {
			dao.search(null);
			fail("Should have thrown NullPointerException.");
		} catch (NullPointerException ex) {}
		try {
			dao.count(null);
			fail("Should have thrown NullPointerException.");
		} catch (NullPointerException ex) {}
		try {
			dao.searchAndCount(null);
			fail("Should have thrown NullPointerException.");
		} catch (NullPointerException ex) {}
		try {
			dao.searchUnique(null);
			fail("Should have thrown NullPointerException.");
		} catch (NullPointerException ex) {}
		s = new Search();
		try {
			dao.search(s);
			fail("Should have thrown NullPointerException.");
		} catch (NullPointerException ex) {}
		try {
			dao.count(s);
			fail("Should have thrown NullPointerException.");
		} catch (NullPointerException ex) {}
		try {
			dao.searchAndCount(s);
			fail("Should have thrown NullPointerException.");
		} catch (NullPointerException ex) {}
		try {
			dao.searchUnique(s);
			fail("Should have thrown NullPointerException.");
		} catch (NullPointerException ex) {}
		

		assertTrue(dao.remove(bob));
		assertEquals(null, dao.find(Person.class, bob.getId()));

		assertTrue(dao.removeById(Person.class, fred.getId()));
		assertEquals(null, dao.find(Person.class, fred.getId()));

		assertEquals(0, dao.count(new Search(Person.class)));

		bob.setId(null);
		fred.setId(null);

		assertTrue(dao.save(bob));
		assertTrue(dao.save(fred));
		
		dao.save(cyndi, marty);
		for (Person p : dao.find(Person.class, cyndi.getId(), bob.getId(), fred.getId())) {
			assertNotNull(p);
		}
		
		dao.removeByIds(Person.class, cyndi.getId(), marty.getId());
		dao.remove(cyndi, fred);
		for (Person p : dao.find(Person.class, cyndi.getId(), marty.getId(), fred.getId())) {
			assertNull(p);
		}
		
		flush();
		clear();
		
		Person bob2 = copy(bob);
		bob2.setFirstName("Bobby");
		assertFalse(dao.save(bob2));

		if (dao == dispatcher) {
			try{
				dao.flush();
				fail("dispatcher should error on flush");
			} catch (DAODispatcherException e) { }
			
			dispatcher.flush(Person.class);
		} else {
			dao.flush();
		}
		
		assertEquals("Bobby", dao.find(bob.getClass(), bob.getId()).getFirstName());
		
		
		dao.refresh(bob2);
		assertTrue(dao.isAttached(bob2));
		assertFalse(dao.isAttached(bob));
		
		Person a = dao.getReference(Person.class, bob2.getId());
		Person b = dao.getReference(Person.class, bob2.getId() + 10);
		
		Person[] pp = dao.getReferences(Person.class, bob2.getId(), bob2.getId() + 10);
		
		assertEquals("Bobby", a.getFirstName());
		assertEquals("Bobby", pp[0].getFirstName());
		
		try {
			b.getFirstName();
			fail("Entity does not exist, should throw error.");
		} catch (ObjectNotFoundException ex) { }
		try {
			pp[1].getFirstName();
			fail("Entity does not exist, should throw error.");
		} catch (ObjectNotFoundException ex) { }
	}

}