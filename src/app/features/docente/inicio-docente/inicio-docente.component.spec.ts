import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { InicioDocenteComponent } from './inicio-docente.component';

describe('InicioDocenteComponent', () => {
  let component: InicioDocenteComponent;
  let fixture: ComponentFixture<InicioDocenteComponent>;

  beforeEach(async () => {
    localStorage.setItem('ugartino-modo', 'local');
    await TestBed.configureTestingModule({
      imports: [InicioDocenteComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(InicioDocenteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});